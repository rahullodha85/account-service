package unit.metrics

import akka.actor.ActorSystem
import helpers.ConfigHelper
import monitoring.{ StatsDClient, StatsDProtocol }
import org.scalatest.prop.PropertyChecks
import org.scalatest.{ Matchers, WordSpec }
import play.api.Configuration
import utils.TestUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StatsDClientSpec extends WordSpec
    with Matchers
    with PropertyChecks {

  private val injector = createIsolatedApplication().build().injector
  val client = new StatsDClient(ConfigHelper, injector.instanceOf[ActorSystem])

  "StatsDProtocol" should {
    "format a string according to the StatsD protocol" in {
      forAll("key", "value", "metric", "sampleRate") { (key: String, value: String, metric: String, sampleRate: Double) =>
        val stat = key + ":" + value + "|" + metric + (if (sampleRate < 1) "|@" + sampleRate else "")
        StatsDProtocol.stat(key, value, metric, sampleRate) should equal(stat)
      }
    }

    "time a future" in {
      client.timeTaken(0, Future { Thread.sleep(50); 6 }).map { time =>
        assert(time >= 50)
      }
    }

    "time something synchronous" in {
      client.timeTaken(0, { val a = 10; a }).map { time =>
        assert(time < 50)
      }
    }
  }
}
