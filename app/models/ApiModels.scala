package models

import java.text.SimpleDateFormat
import java.util.Calendar

import constants.Constants.ZULU_DATE_FORMAT
import helpers.ConfigHelper
import play.api.libs.json._
import play.api.mvc.{ Cookie, RequestHeader }

// results if we are successful
case class ApiResultModel(results: JsValue)

object ApiResultModel {
  implicit val resultFormat = Json.format[ApiResultModel]
}

object EmptyApiResultModel extends ApiResultModel(JsNull)

// For unauthorized responses coming from called services. Needs to be thrown explicitly
case class UnauthorizedException(message: String, cookies: Seq[Cookie]) extends RuntimeException(message)

// error info
case class ApiErrorModel(data: String, error: String)

object ApiErrorModel {
  implicit val errFormat = Json.format[ApiErrorModel]

  // it would be nicer if these were apply methods, but that f's up the implicit conversion
  // we could move the implicits somewhere else.  I tried but didn't care enough.  I wouldn't object if anyone wants to look into it!
  def fromException(ex: Throwable)(implicit request: RequestHeader) = {
    new ApiErrorModel(ex.getMessage, ex.getClass.getSimpleName)
  }

  def fromExceptionAndMessage(message: String, ex: Throwable)(implicit request: RequestHeader) = {
    new ApiErrorModel(message, ex.getClass.getSimpleName)
  }
}

// the request url
case class ApiRequestModel(url: String, server_received_time: String, api_version: String, help: String)

object ApiRequestModel {
  implicit val reqFormat = Json.format[ApiRequestModel]

  def fromReq(request: RequestHeader): ApiRequestModel = {
    val fullRequestUrl = if (request.secure) "https://" else "http://" + request.host + request.uri
    val df = new SimpleDateFormat(ZULU_DATE_FORMAT)
    val versionURI = ConfigHelper.getStringProp("play.http.context")
    val help = if (request.secure) "https://" else "http://" + request.host + versionURI + "/api-docs"
    new ApiRequestModel(
      fullRequestUrl,
      df.format(Calendar.getInstance.getTime),
      versionURI.substring(1),
      help
    )
  }
}

// top level response structure
// I think it needs to go last to pick up the implicit  vals above...
case class ApiModel(request: ApiRequestModel, response: ApiResultModel, errors: Seq[ApiErrorModel])

object ApiModel {
  implicit val apiModelFormat = Json.format[ApiModel]

  // call it with request.body from WS api for example
  def fromBody(body: String): JsResult[ApiModel] =
    Json.parse(body).validate[ApiModel]

  // call this like this: ApiModel.resultsAs[List[String]](res.body)  => JsResult
  // obviously you need to specify the success type you're expecting  :)
  // https://www.playframework.com/documentation/2.3.x/api/scala/index.html#play.api.libs.json.JsResult
  // you can pattern match on JsSuccess/JsError, or map, flatMap, asOpt, asEither ...etc
  def resultsAs[A: Format](body: String): JsResult[A] =
    for {
      apiModel <- fromBody(body)
      result <- apiModel.response.results.validate[A]
    } yield result

  // maybe you just want to proxy the rest of the apiModel, but update the header
  def withHeader(body: String)(implicit req: RequestHeader): ApiModel = {
    val reqModel = ApiRequestModel.fromReq(req)
    fromBody(body)
      .fold(f => throw new Exception(s"unexpected failure parsing ApiModel\n${f.toString}"), _.copy(request = reqModel))
  }
}
