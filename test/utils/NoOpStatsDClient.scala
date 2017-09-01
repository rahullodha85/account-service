package utils

import monitoring.StatsDClientLike
import play.api.mvc.RequestHeader

object NoOpStatsDClient extends StatsDClientLike {
  def timing(key: String, value: Long, sampleRate: Double = 1.0): Boolean = true
  def timing(key: String, value: Long): Unit = ()
  def time[A](tag: String, req: RequestHeader)(f: => A): A = f
  def time[A](tag: String)(f: => A): A = f
  def requestTag(requestHeader: RequestHeader): String = ""
  def decrement(key: String, magnitude: Int = -1, sampleRate: Double = 1.0): Boolean = true
  def decrement(key: String): Unit = ()
  def increment(key: String, magnitude: Int = 1, sampleRate: Double = 1.0): Boolean = true
  def increment(key: String): Unit = ()
  def gauge(key: String, value: String = "1", sampleRate: Double = 1.0): Boolean = true
  def set(key: String, value: Int, sampleRate: Double = 1.0): Boolean = true
  val prefix = "noopClient"
}
