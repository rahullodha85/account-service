/*

Scala implementation of Andrew Gwozdziewycz's StatsdClient.java

Copyright (c) 2013 Joshua Garnett

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package monitoring

import java.io.IOException
import java.net._
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.Random

import akka.actor._
import com.google.inject.ImplementedBy
import javax.inject.{ Inject, Singleton }

import helpers.ConfigHelper
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.api.routing.Router.Tags

import scala.concurrent.{ Future, Promise }

@ImplementedBy(classOf[StatsDClient])
trait StatsDClientLike {
  def timing(key: String, value: Long, sampleRate: Double = 1.0): Boolean
  def timing(key: String, value: Long): Unit
  def time[A](tag: String, req: RequestHeader)(f: => A): A
  def time[A](tag: String)(f: => A): A
  def requestTag(requestHeader: RequestHeader): String
  def decrement(key: String, magnitude: Int = -1, sampleRate: Double = 1.0): Boolean
  def decrement(key: String): Unit
  def increment(key: String, magnitude: Int = 1, sampleRate: Double = 1.0): Boolean
  def increment(key: String): Unit
  def gauge(key: String, value: String = "1", sampleRate: Double = 1.0): Boolean
  def set(key: String, value: Int, sampleRate: Double = 1.0): Boolean
  def prefix: String
}

@Singleton
class StatsDClient @Inject() (config: ConfigHelper, system: ActorSystem) extends StatsDClientLike {

  val multiMetrics = true
  val packetBufferSize = 1024
  val prefix = s"${config.getStringProp("statsd.metric-host")}.${config.getStringProp("statsd.metric-namespace")}."

  private val rand = new Random()

  // prefer use of play's actor system
  private lazy val actorRef = system.actorOf(Props(new StatsDActor(config.getStringProp("statsd.server"), config.getIntProp("statsd.port"), multiMetrics, packetBufferSize, prefix)))

  /**
   * Sends timing stats in milliseconds to StatsD
   *
   * @param key name of the stat
   * @param value time in milliseconds
   */
  def timing(key: String, value: Long, sampleRate: Double = 1.0) = {
    send(key, value.toString, StatsDProtocol.TIMING_METRIC, sampleRate)
  }

  def timing(key: String, value: Long): Unit = {
    timing(key, value, 1.0)
  }

  def getTimeSince(start: Long): Long = System.currentTimeMillis - start

  def requestTag(requestHeader: RequestHeader): String = {
    val controllerActionTag = for {
      controller <- requestHeader.tags.get(Tags.RouteController)
      action <- requestHeader.tags.get(Tags.RouteActionMethod)
    } yield controller.replaceFirst("controllers.", "") + "." + action
    controllerActionTag.getOrElse(requestHeader.path.replaceAll("/", "_"))
  }

  def time[A](tag: String, req: RequestHeader)(f: => A): A = {
    val fullTag = requestTag(req) + (if (tag == "") "" else "." + tag)
    time(fullTag)(f)
  }

  def time[A](tag: String)(f: => A): A = {
    val start = System.currentTimeMillis
    val ret = f
    timeTaken(start, ret).map { tm =>
      timing(tag, tm)
      Logger.info(s"$tag took $tm")
    }
    ret
  }

  // this is theoretically testable by itself
  def timeTaken[A](start: Long, body: A): Future[Long] = {
    val timestamp = Promise[Long]()

    body match {
      case x: Future[_] => x.onComplete { case _ => timestamp.success(getTimeSince(start)) }
      case _            => timestamp.success(getTimeSince(start))
    }

    timestamp.future
  }

  /**
   * Decrement StatsD counter
   *
   * @param key name of the stat
   * @param magnitude how much to decrement
   */
  def decrement(key: String, magnitude: Int = -1, sampleRate: Double = 1.0) = {
    increment(key, magnitude, sampleRate)
  }

  def decrement(key: String): Unit = {
    increment(key, -1, 1.0)
  }

  /**
   * Increment StatsD counter
   *
   * @param key name of the stat
   * @param magnitude how much to increment
   */
  def increment(key: String, magnitude: Int = 1, sampleRate: Double = 1.0) = {
    send(key, magnitude.toString, StatsDProtocol.COUNTER_METRIC, sampleRate)
  }

  def increment(key: String): Unit = {
    increment(key, 1, 1.0)
  }

  /**
   * StatsD now also supports gauges, arbitrary values, which can be recorded.
   *
   * @param key name of the stat
   * @param value Can be a fixed value or increase or decrease (Ex: "10" "-1" "+5")
   */
  def gauge(key: String, value: String = "1", sampleRate: Double = 1.0) = {
    send(key, value, StatsDProtocol.GAUGE_METRIC, sampleRate)
  }

  /**
   * StatsD supports counting unique occurrences of events between flushes, using a Set to store all occurring events.
   *
   * @param key name of the stat
   * @param value value of the set
   */
  def set(key: String, value: Int, sampleRate: Double = 1.0) = {
    send(key, value.toString, StatsDProtocol.SET_METRIC, sampleRate)
  }

  /**
   * Checks the sample rate and sends the stat to the actor if it passes
   */
  private def send(key: String, value: String, metric: String, sampleRate: Double): Boolean = {
    if (sampleRate >= 1 || rand.nextDouble <= sampleRate) {

      actorRef ! SendStat(StatsDProtocol.stat(key, value, metric, sampleRate))
      true
    } else {
      false
    }
  }
}

object StatsDProtocol {
  val TIMING_METRIC = "ms"
  val COUNTER_METRIC = "c"
  val GAUGE_METRIC = "g"
  val SET_METRIC = "s"

  /**
   * @return Returns a string that conforms to the StatsD protocol:
   *         KEY:VALUE|METRIC or KEY:VALUE|METRIC|@SAMPLE_RATE
   */
  def stat(key: String, value: String, metric: String, sampleRate: Double) = {
    val sampleRateString = if (sampleRate < 1) "|@" + sampleRate else ""
    key + ":" + value + "|" + metric + sampleRateString
  }
}

/**
 * Message for the StatsDActor
 */
private case class SendStat(stat: String)

/**
 * @param host The statsd host
 * @param port The statsd port
 * @param multiMetrics If true, multiple stats will be sent in a single UDP packet
 * @param packetBufferSize If multiMetrics is true, this is the max buffer size before sending the UDP packet
 */
private class StatsDActor(
    host:             String,
    port:             Int,
    multiMetrics:     Boolean,
    packetBufferSize: Int,
    prefix:           String
) extends Actor {

  private val sendBuffer = ByteBuffer.allocate(packetBufferSize)

  private val address = new InetSocketAddress(InetAddress.getByName(host), port)
  private val channel = DatagramChannel.open()

  def receive = {
    case msg: SendStat => doSend(msg.stat)
    case _             => Logger.error("Unknown message")
  }

  override def postStop() = {
    //save any remaining data to StatsD
    flush()

    //Close the channel
    if (channel.isOpen) {
      channel.close()
    }

    sendBuffer.clear()
  }

  private def doSend(stat: String) = {
    try {
      val data = (prefix + stat).getBytes("utf-8")

      // If we're going to go past the threshold of the buffer then flush.
      // the +1 is for the potential '\n' in multi_metrics below
      if (sendBuffer.remaining() < (data.length + 1)) {
        flush()
      }

      // multiple metrics are separated by '\n'
      if (sendBuffer.position() > 0) {
        sendBuffer.put('\n'.asInstanceOf[Byte])
      }

      // append the data
      sendBuffer.put(data)

      if (!multiMetrics) {
        flush()
      }

    } catch {
      case e: IOException =>
        Logger.error("Could not send stat {} to host {}:{}", sendBuffer.toString, address.getHostName, address.getPort.toString, e)
    }
  }

  private def flush(): Unit = {
    try {
      val sizeOfBuffer = sendBuffer.position()

      if (sizeOfBuffer <= 0) {
        // empty buffer
        return
      }

      // send and reset the buffer
      sendBuffer.flip()
      val nbSentBytes = channel.send(sendBuffer, address)
      sendBuffer.limit(sendBuffer.capacity())
      sendBuffer.rewind()

      if (sizeOfBuffer != nbSentBytes) {
        Logger.error("Could not send entirely stat {} to host {}:{}. Only sent {} bytes out of {} bytes", sendBuffer.toString,
          address.getHostName, address.getPort.toString, nbSentBytes.toString, sizeOfBuffer.toString)
      }

    } catch {
      case e: IOException =>
        Logger.error("Could not send stat {} to host {}:{}", sendBuffer.toString, address.getHostName, address.getPort.toString, e)
    }
  }
}
