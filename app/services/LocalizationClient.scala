package services

import java.net.ConnectException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import helpers.ConfigHelper
import play.Logger
import play.api.libs.json._
import play.api.libs.ws.WSClient

import scala.concurrent.duration.Duration
import scala.concurrent.{ Future, TimeoutException }
import scala.language.postfixOps

class LocalizationClient @Inject() (client: WSClient, configHelper: ConfigHelper) {

  import play.api.libs.concurrent.Execution.Implicits._
  final val serviceTimeout: Duration = Duration(configHelper.getIntProp("data-service.timeout"), TimeUnit.MILLISECONDS)

  def get[T](url: String, validate: (JsValue) => (JsResult[Seq[T]]), hdrs: Option[(String, String)] = None): Future[Seq[T]] = {
    client
      .url(url)
      .withHeaders(hdrs.getOrElse(("Cookie", "")))
      .withRequestTimeout(serviceTimeout)
      .get()
      .map {
        response =>
          response.status match {
            case 200 =>
              val results = (response.json \ "response" \ "results").getOrElse(JsNull)
              validate(results) match {
                case JsSuccess(values, _) => values
                case JsError(e) =>
                  Logger.error(s"Error serializing the json response $e")
                  throw JsonSerializationException(s"Error serializing the json response $e")
              }
            case _ =>
              Logger.error(s"Service returned with an error response response=${response.body} status code=${response.status}")
              throw ServiceException(s"GET $url returned error status code=${response.status}")
          }
      } recover {
        case e: ConnectException =>
          Logger.error(s"$url un reachable", e)
          throw ConnectionException(s"$url un reachable")
        case e: TimeoutException =>
          Logger.error(s"GET $url Timedout", e)
          throw e
        case e: Throwable =>
          Logger.error(s"GET $url failed: unknown exception occurred while calling service", e)
          throw e
      }
  }
}

case class JsonSerializationException(message: String) extends Exception(message)
case class ServiceException(message: String) extends Exception(message)
case class ConnectionException(message: String) extends Exception(message)
