package services

import java.net.ConnectException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import com.fasterxml.jackson.core.JsonParseException
import constants.Constants._
import helpers.{ AccountHelper, ConfigHelper }
import models._
import monitoring.StatsDClientLike
import net.logstash.logback.argument.StructuredArguments._
import play.Logger
import play.api.data.validation.ValidationError
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws.{ WSClient, WSCookie, WSResponse }
import play.api.mvc.Cookie
import validations.Validator

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{ Failure, Success, Try }

class HttpTransportService @Inject() (client: WSClient, statsClient: StatsDClientLike, configHelper: ConfigHelper, validator: Validator, accountHelper: AccountHelper) {

  final val serviceTimeout: Duration = Duration(configHelper.getIntProp("data-service.timeout"), TimeUnit.MILLISECONDS)
  final val GENERIC_FAILURE_RESPONSE = FailureResponse(Seq(ApiErrorModel(GENERIC_ERROR_MESSAGE, GENERIC_ERROR)), 500)
  val headersSetForPost = Set("Content-Length", "Content-Type", "Accept-Encoding")
  type Parser[ResponseType] = (String, Option[String], WSResponse, JsValue, Reads[ResponseType]) => Either[FailureResponse, SuccessfulResponse[ResponseType]]

  def postToService[RequestType, ResponseType](serviceUrl: String, resource: Option[String], payload: RequestType, headers: Map[String, String], parameters: Map[String, String] = Map.empty[String, String])(implicit requestWrites: Writes[RequestType], responseReads: Reads[ResponseType]): Future[Either[FailureResponse, SuccessfulResponse[ResponseType]]] = {
    val request = client
      .url(constructUrl(serviceUrl, resource))
      .withQueryString(parameters.toSeq: _*)
      .withRequestTimeout(serviceTimeout)
      .withHeaders(headers.toSeq: _*)
      .post(Json.toJson[RequestType](payload))
    processServiceResponse(serviceUrl, resource, request, parseAPIModel)
  }

  def putToService[RequestType, ResponseType](serviceUrl: String, resource: Option[String], payload: RequestType, headers: Map[String, String], parameters: Map[String, String] = Map.empty[String, String])(implicit requestWrites: Writes[RequestType], responseReads: Reads[ResponseType]): Future[Either[FailureResponse, SuccessfulResponse[ResponseType]]] = {
    val request = client
      .url(constructUrl(serviceUrl, resource))
      .withQueryString(parameters.toSeq: _*)
      .withRequestTimeout(serviceTimeout)
      .withHeaders(headers.toSeq: _*)
      .put(Json.toJson[RequestType](payload))
    processServiceResponse(serviceUrl, resource, request, parseAPIModel)
  }

  def deleteFromService[ResponseType](serviceUrl: String, resource: Option[String], headers: Map[String, String], parameters: Map[String, String] = Map.empty[String, String])(implicit responseReads: Reads[ResponseType]): Future[Either[FailureResponse, SuccessfulResponse[ResponseType]]] = {
    val request = client
      .url(constructUrl(serviceUrl, resource))
      .withQueryString(parameters.toSeq: _*)
      .withRequestTimeout(serviceTimeout)
      .withHeaders(headers.toSeq: _*)
      .delete()
    processServiceResponse(serviceUrl, resource, request, parseAPIModel)
  }

  def getFromService[ResponseType](serviceUrl: String, resource: Option[String], headers: Map[String, String], parameters: Map[String, String] = Map.empty[String, String])(implicit responseReads: Reads[ResponseType]): Future[Either[FailureResponse, SuccessfulResponse[ResponseType]]] = {
    val getHeaders = headers.filter { case (key, value) => !headersSetForPost.contains(key) }
    val request = client
      .url(constructUrl(serviceUrl, resource))
      .withQueryString(parameters.toSeq: _*)
      .withRequestTimeout(serviceTimeout)
      .withHeaders(getHeaders.toSeq: _*)
      .get()
    processServiceResponse(serviceUrl, resource, request, parseAPIModel)
  }

  def postToExternalService[RequestType, ResponseType](serviceUrl: String, resource: Option[String], payload: RequestType, headers: Map[String, String], parameters: Map[String, String] = Map.empty[String, String])(implicit requestWrites: Writes[RequestType], responseReads: Reads[ResponseType]): Future[Either[FailureResponse, SuccessfulResponse[ResponseType]]] = {
    val request = client
      .url(constructUrl(serviceUrl, resource))
      .withQueryString(parameters.toSeq: _*)
      .withRequestTimeout(serviceTimeout)
      .withHeaders(headers.toSeq: _*)
      .post(Json.toJson[RequestType](payload))
    processServiceResponse(serviceUrl, resource, request, parseResponseModel)
  }
  private def constructUrl(serviceUrl: String, resource: Option[String]): String = {
    resource match {
      case Some(resourcePath) => serviceUrl + "/" + resourcePath
      case None               => serviceUrl
    }
  }

  def processServiceResponse[ResponseType](serviceUrl: String, resource: Option[String], response: Future[WSResponse], parseFunction: Parser[ResponseType] = parseAPIModel[ResponseType])(implicit responseReads: Reads[ResponseType]): Future[Either[FailureResponse, SuccessfulResponse[ResponseType]]] = {
    response.map { response =>
      Logger.debug(s"$serviceUrl/${resource.getOrElse("")} response", keyValue("response_body", response.body))
      Try {
        response.json
      } match {
        case Success(json) =>
          parseFunction(serviceUrl, resource, response, json, responseReads)
        case Failure(e: JsonParseException) =>
          Logger.warn(s"$serviceUrl/${resource.getOrElse("")} returned non json response", keyValue("response_body", response.body), e)
          throw ServiceException(s"Service responded with error status_code ${response.status}")
        case Failure(e: Throwable) =>
          throw e
      }
    } recover {
      case e: UnauthorizedException =>
        throw e
      case e: ConnectException =>
        statsClient.increment(s"http_calls.${resource.getOrElse("")}-un-reachable")
        handleThrowable(s"$serviceUrl/${resource.getOrElse("")} un reachable", e)
      case e: TimeoutException =>
        statsClient.increment(s"http_calls.${resource.getOrElse("")}_timeout")
        handleThrowable(s"$serviceUrl/${resource.getOrElse("")} Timeout received", e)
      case e: ServiceException =>
        handleThrowable(s"$serviceUrl/${resource.getOrElse("")} ${e.getMessage}", e)
      case e: Throwable =>
        handleThrowable(s"$serviceUrl/${resource.getOrElse("")} failed: unknown exception occurred while calling service", e)
    }
  }

  private def parseAPIModel[ResponseType]: Parser[ResponseType] = (serviceUrl, resource, response, json, responseReads) => {
    implicit val reads = responseReads
    json.validate[ApiModel] match {
      case JsSuccess(apiModel, path) => {
        if (apiModel.errors.isEmpty) {
          parseResponseModel[ResponseType](serviceUrl, resource, response, apiModel.response.results, reads)
        } else {
          apiModel.errors.foreach { error => statsClient.increment(s"http_calls.${resource.getOrElse("")}_${error.error}") }
          Logger.info(s"$serviceUrl/${resource.getOrElse("")} responded with errors", keyValue("errors", Json.toJson(apiModel.errors).toString()))

          val unauthorizedErrors = apiModel.errors.find(errorPair => errorPair.error == ERROR_USER_NOT_LOGGED_IN || errorPair.data == ERROR_DATA_SESSION_EXPIRED)
          unauthorizedErrors.foreach(_ => throw UnauthorizedException("User is not logged in", processCookies(response.cookies)))

          Logger.error(s"$serviceUrl/${resource.getOrElse("")} call failed", keyValue("status_code", response.status), keyValue("errors", Json.toJson(apiModel.errors).toString()))
          val clientFacingErrors = accountHelper.getBrandSpecificErrorMessage(apiModel.errors, resource, response.status)
          val clientFacingStatus = if (response.status >= 500) { 500 } else { response.status }
          Left(FailureResponse(clientFacingErrors, clientFacingStatus, processCookies(response.cookies)))
        }
      }
      case JsError(errors) => {
        handleParsingErrors(errors, s"$resource-service responded with an invalid response", response.body)
      }
    }
  }

  private def parseResponseModel[ResponseType]: Parser[ResponseType] = (serviceUrl, resource, response, json, responseReads) => {
    implicit val reads = responseReads
    json.validate[ResponseType] match {
      case JsSuccess(domainModel, path) =>
        Logger.info(s"$serviceUrl/${resource.getOrElse("")} response parsed successfully")
        Right(SuccessfulResponse(domainModel, processCookies(response.cookies)))
      case JsError(errors) =>
        handleParsingErrors(errors, s"$serviceUrl/${resource.getOrElse("")}: unable to parse ${resource.getOrElse("")} response", response.body)
    }
  }

  private def handleThrowable(message: String, e: Throwable): Left[FailureResponse, Nothing] = {
    Logger.error(message, e)
    if (configHelper.getBooleanProp(DEV_MODE)) {
      Left(FailureResponse(Seq(ApiErrorModel(message, e.getMessage)), 500))
    } else {
      Left(GENERIC_FAILURE_RESPONSE)
    }
  }

  private def handleParsingErrors(errors: Seq[(JsPath, Seq[ValidationError])], message: String, responseBody: String): Left[FailureResponse, Nothing] = {
    Logger.error(message, keyValue("response_body", responseBody))
    if (configHelper.getBooleanProp(DEV_MODE)) {
      Left(FailureResponse(validator.transformToApiErrors(errors), 500))
    } else {
      Left(GENERIC_FAILURE_RESPONSE)
    }
  }

  private def processCookies(wsCookies: Seq[WSCookie]): Seq[Cookie] = {
    if (wsCookies != null) {
      wsCookies.map { c =>
        val domain: Option[String] = c.domain match {
          case null  => None
          case s @ _ => Some(s)
        }
        Cookie(c.name.getOrElse(""), c.value.getOrElse(""), c.maxAge.map(_.toInt), c.path, domain, c.secure, c.underlying[org.asynchttpclient.cookie.Cookie].isHttpOnly)
      }.filter(_.name.nonEmpty)
    } else {
      Seq.empty
    }
  }
}
