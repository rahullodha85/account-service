package unit.service

import java.util.concurrent.TimeUnit

import constants.Constants._
import helpers.{ AccountHelper, ConfigHelper }
import models._
import models.servicemodel.{ MemberInfo, UserAccount }
import models.website.SaksFirstModel
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.mock.MockitoSugar
import play.api.data.validation.ValidationError
import play.api.http.Writeable
import play.api.libs.json.{ JsPath, JsValue, Json }
import play.api.libs.ws.{ WSClient, WSCookie, WSRequest, WSResponse }
import play.api.mvc.Cookie
import services.HttpTransportService
import utils.NoOpStatsDClient
import utils.TestUtils._
import validations.Validator

import scala.concurrent._
import scala.concurrent.duration.Duration

class HttpTransportServiceSpec extends WordSpec with MockitoSugar {

  private val injector = createIsolatedApplication().build().injector
  val accountHelper = injector.instanceOf[AccountHelper]
  val validator = injector.instanceOf[Validator]

  private def createWSCookie(httpOnly: Boolean = false): WSCookie = {
    val mockUnderlying = mock[org.asynchttpclient.cookie.Cookie]
    when(mockUnderlying.isHttpOnly).thenReturn(httpOnly)

    new WSCookie {
      override def underlying[T]: T = mockUnderlying.asInstanceOf[T]

      override def domain: String = "who cares"

      override def maxAge: Option[Long] = Some(12345)

      override def secure: Boolean = true

      override def value: Option[String] = Some("r3iuheui3902")

      override def name: Option[String] = Some("JSESSIONID")

      override def path: String = "who cares"

    }
  }

  "HttpTransportService" should {
    "call WS with correct post body, parameters, and headers" in {
      val wsClient = mock[WSClient](withSettings.defaultAnswer(RETURNS_DEEP_STUBS))
      val requestHolder = mock[WSRequest]
      when(wsClient.url(any[String])).thenReturn(requestHolder)
      when(requestHolder.withQueryString(any[(String, String)])).thenReturn(requestHolder)
      when(requestHolder.withRequestTimeout(any[Duration])).thenReturn(requestHolder)
      when(requestHolder.withHeaders(any[(String, String)])).thenReturn(requestHolder)
      when(requestHolder.post(any[JsValue])(any[Writeable[JsValue]])).thenReturn(Future.successful(mock[WSResponse]))

      val httpTransportService = new HttpTransportService(wsClient, NoOpStatsDClient, ConfigHelper, validator, accountHelper)
      httpTransportService.postToService[JsValue, JsValue]("serviceUrl", Some("resource"), Json.parse("""{"some": "stuff"}"""), Map("headerKey" -> "headerValue"), Map("key" -> "value"))

      verify(wsClient).url("serviceUrl/resource")
      verify(requestHolder).withQueryString(("key", "value"))
      verify(requestHolder).withRequestTimeout(Duration(ConfigHelper.getIntProp("data-service.timeout"), TimeUnit.MILLISECONDS))
      verify(requestHolder).withHeaders(("headerKey", "headerValue"))
      verify(requestHolder).post(Matchers.eq[JsValue](Json.parse("""{"some": "stuff"}""")))(any[Writeable[JsValue]])
    }

    "call WS put with correct body, parameters, and headers" in {
      val wsClient = mock[WSClient](withSettings.defaultAnswer(RETURNS_DEEP_STUBS))
      val requestHolder = mock[WSRequest]
      when(wsClient.url(any[String])).thenReturn(requestHolder)
      when(requestHolder.withQueryString(any[(String, String)])).thenReturn(requestHolder)
      when(requestHolder.withRequestTimeout(any[Duration])).thenReturn(requestHolder)
      when(requestHolder.withHeaders(any[(String, String)])).thenReturn(requestHolder)
      when(requestHolder.put(any[JsValue])(any[Writeable[JsValue]])).thenReturn(Future.successful(mock[WSResponse]))

      val httpTransportService = new HttpTransportService(wsClient, NoOpStatsDClient, ConfigHelper, validator, accountHelper)
      httpTransportService.putToService[JsValue, JsValue]("serviceUrl", Some("resource"), Json.parse("""{"some": "stuff"}"""), Map("headerKey" -> "headerValue"), Map("key" -> "value"))

      verify(wsClient).url("serviceUrl/resource")
      verify(requestHolder).withQueryString(("key", "value"))
      verify(requestHolder).withRequestTimeout(Duration(ConfigHelper.getIntProp("data-service.timeout"), TimeUnit.MILLISECONDS))
      verify(requestHolder).withHeaders(("headerKey", "headerValue"))
      verify(requestHolder).put(Matchers.eq[JsValue](Json.parse("""{"some": "stuff"}""")))(any[Writeable[JsValue]])
    }

    "call WS delete with correct post body, parameters, and headers" in {
      val wsClient = mock[WSClient](withSettings.defaultAnswer(RETURNS_DEEP_STUBS))
      val requestHolder = mock[WSRequest]
      when(wsClient.url(any[String])).thenReturn(requestHolder)
      when(requestHolder.withQueryString(any[(String, String)])).thenReturn(requestHolder)
      when(requestHolder.withRequestTimeout(any[Duration])).thenReturn(requestHolder)
      when(requestHolder.withHeaders(any[(String, String)])).thenReturn(requestHolder)
      when(requestHolder.delete()).thenReturn(Future.successful(mock[WSResponse]))

      val httpTransportService = new HttpTransportService(wsClient, NoOpStatsDClient, ConfigHelper, validator, accountHelper)
      httpTransportService.deleteFromService[JsValue]("serviceUrl", Some("resource/id"), Map("headerKey" -> "headerValue"), Map("key" -> "value"))

      verify(wsClient).url("serviceUrl/resource/id")
      verify(requestHolder).withQueryString(("key", "value"))
      verify(requestHolder).withRequestTimeout(Duration(ConfigHelper.getIntProp("data-service.timeout"), TimeUnit.MILLISECONDS))
      verify(requestHolder).withHeaders(("headerKey", "headerValue"))
      verify(requestHolder).delete()
    }

    "call WS without resource or slash in url if not given" in {
      val wsClient = mock[WSClient](withSettings.defaultAnswer(RETURNS_DEEP_STUBS))
      val requestHolder = mock[WSRequest]
      when(wsClient.url(any[String])).thenReturn(requestHolder)
      when(requestHolder.withQueryString(any[(String, String)])).thenReturn(requestHolder)
      when(requestHolder.withRequestTimeout(any[Duration])).thenReturn(requestHolder)
      when(requestHolder.withHeaders(any[(String, String)])).thenReturn(requestHolder)
      when(requestHolder.post(any[JsValue])(any[Writeable[JsValue]])).thenReturn(Future.successful(mock[WSResponse]))

      val httpTransportService = new HttpTransportService(wsClient, NoOpStatsDClient, ConfigHelper, validator, accountHelper)
      httpTransportService.postToService[JsValue, JsValue]("serviceUrl", None, Json.parse("""{"some": "stuff"}"""), Map("headerKey" -> "headerValue"), Map("key" -> "value"))

      verify(wsClient).url("serviceUrl")
      verify(requestHolder).withQueryString(("key", "value"))
      verify(requestHolder).withRequestTimeout(Duration(ConfigHelper.getIntProp("data-service.timeout"), TimeUnit.MILLISECONDS))
      verify(requestHolder).withHeaders(("headerKey", "headerValue"))
      verify(requestHolder).post(Matchers.eq[JsValue](Json.parse("""{"some": "stuff"}""")))(any[Writeable[JsValue]])
    }

    "call WS with correct post body, default empty parameters, and headers when no parameters are passed" in {
      val wsClient = mock[WSClient](withSettings.defaultAnswer(RETURNS_DEEP_STUBS))
      val requestHolder = mock[WSRequest]
      when(wsClient.url(any[String])).thenReturn(requestHolder)
      when(requestHolder.withQueryString(any[(String, String)])).thenReturn(requestHolder)
      when(requestHolder.withRequestTimeout(any[Duration])).thenReturn(requestHolder)
      when(requestHolder.withHeaders(any[(String, String)])).thenReturn(requestHolder)
      when(requestHolder.post(any[JsValue])(any[Writeable[JsValue]])).thenReturn(Future.successful(mock[WSResponse]))

      val httpTransportService = new HttpTransportService(wsClient, NoOpStatsDClient, ConfigHelper, validator, accountHelper)
      httpTransportService.postToService[JsValue, JsValue]("serviceUrl", Some("resource"), Json.parse("""{"some": "stuff"}"""), Map("headerKey" -> "headerValue"))

      verify(wsClient).url("serviceUrl/resource")
      verify(requestHolder).withQueryString(Seq.empty: _*)
      verify(requestHolder).withRequestTimeout(Duration(ConfigHelper.getIntProp("data-service.timeout"), TimeUnit.MILLISECONDS))
      verify(requestHolder).withHeaders(("headerKey", "headerValue"))
      verify(requestHolder).post(Matchers.eq[JsValue](Json.parse("""{"some": "stuff"}""")))(any[Writeable[JsValue]])
    }

    "call WS get with correct parameters, and headers" in {
      val wsClient = mock[WSClient](withSettings.defaultAnswer(RETURNS_DEEP_STUBS))
      val requestHolder = mock[WSRequest]
      when(wsClient.url(any[String])).thenReturn(requestHolder)
      when(requestHolder.withQueryString(any[(String, String)])).thenReturn(requestHolder)
      when(requestHolder.withRequestTimeout(any[Duration])).thenReturn(requestHolder)
      when(requestHolder.withHeaders(any[(String, String)])).thenReturn(requestHolder)
      when(requestHolder.get()).thenReturn(Future.successful(mock[WSResponse]))

      val httpTransportService = new HttpTransportService(wsClient, NoOpStatsDClient, ConfigHelper, validator, accountHelper)
      httpTransportService.getFromService[JsValue]("serviceUrl", Some("resource"), Map("headerKey" -> "headerValue"), Map("key" -> "value"))

      verify(wsClient).url("serviceUrl/resource")
      verify(requestHolder).withQueryString(("key", "value"))
      verify(requestHolder).withRequestTimeout(Duration(ConfigHelper.getIntProp("data-service.timeout"), TimeUnit.MILLISECONDS))
      verify(requestHolder).withHeaders(("headerKey", "headerValue"))
      verify(requestHolder).get()
    }

    "call WS get without resource or slash in url if not given" in {
      val wsClient = mock[WSClient](withSettings.defaultAnswer(RETURNS_DEEP_STUBS))
      val requestHolder = mock[WSRequest]
      when(wsClient.url(any[String])).thenReturn(requestHolder)
      when(requestHolder.withQueryString(any[(String, String)])).thenReturn(requestHolder)
      when(requestHolder.withRequestTimeout(any[Duration])).thenReturn(requestHolder)
      when(requestHolder.withHeaders(any[(String, String)])).thenReturn(requestHolder)
      when(requestHolder.get()).thenReturn(Future.successful(mock[WSResponse]))

      val httpTransportService = new HttpTransportService(wsClient, NoOpStatsDClient, ConfigHelper, validator, accountHelper)
      httpTransportService.getFromService[JsValue]("serviceUrl", None, Map("headerKey" -> "headerValue"), Map("key" -> "value"))

      verify(wsClient).url("serviceUrl")
      verify(requestHolder).withQueryString(("key", "value"))
      verify(requestHolder).withRequestTimeout(Duration(ConfigHelper.getIntProp("data-service.timeout"), TimeUnit.MILLISECONDS))
      verify(requestHolder).withHeaders(("headerKey", "headerValue"))
      verify(requestHolder).get()
    }

    "call WS get with default empty parameters, and headers when no parameters are passed" in {
      val wsClient = mock[WSClient](withSettings.defaultAnswer(RETURNS_DEEP_STUBS))
      val requestHolder = mock[WSRequest]
      when(wsClient.url(any[String])).thenReturn(requestHolder)
      when(requestHolder.withQueryString(any[(String, String)])).thenReturn(requestHolder)
      when(requestHolder.withRequestTimeout(any[Duration])).thenReturn(requestHolder)
      when(requestHolder.withHeaders(any[(String, String)])).thenReturn(requestHolder)
      when(requestHolder.get()).thenReturn(Future.successful(mock[WSResponse]))

      val httpTransportService = new HttpTransportService(wsClient, NoOpStatsDClient, ConfigHelper, validator, accountHelper)
      httpTransportService.getFromService[JsValue]("serviceUrl", Some("resource"), Map("headerKey" -> "headerValue"))

      verify(wsClient).url("serviceUrl/resource")
      verify(requestHolder).withQueryString(Seq.empty: _*)
      verify(requestHolder).withRequestTimeout(Duration(ConfigHelper.getIntProp("data-service.timeout"), TimeUnit.MILLISECONDS))
      verify(requestHolder).withHeaders(("headerKey", "headerValue"))
      verify(requestHolder).get()
    }

    "throw exception and log response body when status code is greater or equal than 500" in {
      val configHelper = mock[ConfigHelper]
      when(configHelper.getBooleanProp(DEV_MODE)).thenReturn(true)
      val httpTransportService = new HttpTransportService(mock[WSClient], NoOpStatsDClient, configHelper, validator, accountHelper)
      val wsResponse = mock[WSResponse]
      when(wsResponse.status).thenReturn(500)
      when(wsResponse.body).thenReturn("<some response>")

      val response = httpTransportService.processServiceResponse[UserAccount]("url", Some("resource"), Future.successful(wsResponse))

      whenReady(response) { actualResponse =>
        actualResponse shouldNot be(Left(FailureResponse(Seq(ApiErrorModel(GENERIC_ERROR_MESSAGE, GENERIC_ERROR)), 500)))
      }
    }

    "transform a successful response" in {
      val httpTransportService = new HttpTransportService(mock[WSClient], NoOpStatsDClient, ConfigHelper, validator, accountHelper)

      val wsResponse = mock[WSResponse]
      val wSCookie: WSCookie = createWSCookie()
      val userAccount: UserAccount = new UserAccount(Some("12345"), "abc@email.test", "first", Some("555-121-2121"), "abc@email.test", Some("middle"), "last", Some("a uuid"))
      val cookie: Cookie = new Cookie(wSCookie.name.get, wSCookie.value.get, wSCookie.maxAge.map(_.toInt), wSCookie.path, Some(wSCookie.domain), wSCookie.secure, wSCookie.underlying[org.asynchttpclient.cookie.Cookie].isHttpOnly)
      val apiResponse = ApiModel(ApiRequestModel("", "", "", ""), ApiResultModel(results = Json.toJson(userAccount)), Seq.empty[ApiErrorModel])
      val cookies: Seq[WSCookie] = Seq(wSCookie)

      when(wsResponse.json).thenReturn(Json.toJson(apiResponse))
      when(wsResponse.cookies).thenReturn(cookies)

      val response = httpTransportService.processServiceResponse[UserAccount]("", Some(""), Future.successful(wsResponse))

      whenReady(response) { actualResponse =>
        actualResponse shouldBe Right(SuccessfulResponse(userAccount, Seq(cookie)))
      }
    }

    "return a generic left error message in the case of a unserializable body" in {
      val httpTransportService = new HttpTransportService(mock[WSClient], NoOpStatsDClient, ConfigHelper, validator, accountHelper)

      val wsResponse = mock[WSResponse]
      val wSCookie: WSCookie = createWSCookie()
      val apiResponse = ApiModel(ApiRequestModel("", "", "", ""), ApiResultModel(results = Json.toJson(new SaksFirstModel(true, true, MemberInfo.empty))), Seq.empty[ApiErrorModel])
      val cookies: Seq[WSCookie] = Seq(wSCookie)

      when(wsResponse.json).thenReturn(Json.toJson(apiResponse))
      when(wsResponse.cookies).thenReturn(cookies)
      when(wsResponse.status).thenReturn(200)

      val response = httpTransportService.processServiceResponse[UserAccount]("", Some(""), Future.successful(wsResponse))

      whenReady(response) { actualResponse =>
        actualResponse shouldBe Left(FailureResponse(Seq(ApiErrorModel(GENERIC_ERROR_MESSAGE, GENERIC_ERROR)), 500))
      }
    }

    "return validation erros in dev mode in the case of a unserializable body" in {
      val validator = mock[Validator]
      val configHelper = mock[ConfigHelper]
      when(configHelper.getBooleanProp(DEV_MODE)).thenReturn(true)
      val httpTransportService = new HttpTransportService(mock[WSClient], NoOpStatsDClient, configHelper, validator, accountHelper)

      val wsResponse = mock[WSResponse]
      val wSCookie: WSCookie = createWSCookie()
      val apiResponse = Json.parse("{}")
      val cookies: Seq[WSCookie] = Seq(wSCookie)

      when(wsResponse.json).thenReturn(apiResponse)
      when(wsResponse.cookies).thenReturn(cookies)
      when(wsResponse.status).thenReturn(200)

      val response = httpTransportService.processServiceResponse[SaksFirstModel]("", Some(""), Future.successful(wsResponse))

      whenReady(response) { actualResponse =>
        verify(validator).transformToApiErrors(any[Seq[(JsPath, Seq[ValidationError])]])
        actualResponse.left.get.code shouldBe 500
      }
    }

    "return validation erros in dev mode in the case of a unexpected body type" in {
      val validator = mock[Validator]
      val configHelper = mock[ConfigHelper]
      when(configHelper.getBooleanProp(DEV_MODE)).thenReturn(true)
      val httpTransportService = new HttpTransportService(mock[WSClient], NoOpStatsDClient, configHelper, validator, accountHelper)

      val wsResponse = mock[WSResponse]
      val wSCookie: WSCookie = createWSCookie()
      val apiResponse = ApiModel(ApiRequestModel("", "", "", ""), ApiResultModel(results = Json.toJson(new SaksFirstModel(true, true, MemberInfo.empty))), Seq.empty[ApiErrorModel])
      val cookies: Seq[WSCookie] = Seq(wSCookie)

      when(wsResponse.json).thenReturn(Json.toJson(apiResponse))
      when(wsResponse.cookies).thenReturn(cookies)
      when(wsResponse.status).thenReturn(200)

      val response = httpTransportService.processServiceResponse[UserAccount]("", Some(""), Future.successful(wsResponse))

      whenReady(response) { actualResponse =>
        verify(validator).transformToApiErrors(any[Seq[(JsPath, Seq[ValidationError])]])
        actualResponse.left.get.code shouldBe 500
      }
    }

    "transform an error response into user facing error response with status code if response code is not 500 series and return cookies" in {
      val httpTransportService = new HttpTransportService(mock[WSClient], NoOpStatsDClient, ConfigHelper, validator, accountHelper)

      val wsResponse = mock[WSResponse]
      val wSCookie: WSCookie = createWSCookie()
      val apiResponse = ApiModel(ApiRequestModel("", "", "", ""), ApiResultModel(results = Json.parse("{}")), Seq(ApiErrorModel("data", "ERROR_INVALID_BILLING_ZIP_CODE")))
      val cookies: Seq[WSCookie] = Seq(wSCookie)

      when(wsResponse.json).thenReturn(Json.toJson(apiResponse))
      when(wsResponse.cookies).thenReturn(cookies)
      when(wsResponse.status).thenReturn(400)

      val response = httpTransportService.processServiceResponse[UserAccount]("", Some(""), Future.successful(wsResponse))

      whenReady(response) { actualResponse =>
        val message = "We were not able to find your order. Please check your order number and billing address zip code and try again. If you continue to have problems, please call 1.866.601.5105."
        val cookie = Cookie("JSESSIONID", "r3iuheui3902", Some(12345), "who cares", Some("who cares"), true, false)
        actualResponse shouldBe Left(FailureResponse(Seq(ApiErrorModel(message, "global_message.ERROR_INVALID_BILLING_ZIP_CODE")), 400, Seq(cookie)))
      }
    }

    "transform any unexpected 500+ failure response into a generic left error response" in {
      val httpTransportService = new HttpTransportService(mock[WSClient], NoOpStatsDClient, ConfigHelper, validator, accountHelper)

      val wsResponse = mock[WSResponse]
      val wSCookie: WSCookie = createWSCookie()
      val apiResponse = ApiModel(ApiRequestModel("", "", "", ""), ApiResultModel(results = Json.parse("{}")), Seq(ApiErrorModel("data", "error")))
      val cookies: Seq[WSCookie] = Seq(wSCookie)

      when(wsResponse.json).thenReturn(Json.toJson(apiResponse))
      when(wsResponse.cookies).thenReturn(cookies)
      when(wsResponse.status).thenReturn(503)

      val response = httpTransportService.processServiceResponse[UserAccount]("", Some(""), Future.successful(wsResponse))

      whenReady(response) { actualResponse =>
        val cookie = Cookie("JSESSIONID", "r3iuheui3902", Some(12345), "who cares", Some("who cares"), true, false)
        actualResponse shouldBe Left(FailureResponse(Seq(ApiErrorModel(GENERIC_ERROR_MESSAGE, GENERIC_ERROR)), 500, Seq(cookie)))
      }
    }

    "throw an unauthorized exception if user is not logged in" in {
      val httpTransportService = new HttpTransportService(mock[WSClient], NoOpStatsDClient, ConfigHelper, validator, accountHelper)

      val wsResponse = mock[WSResponse]
      val wSCookie: WSCookie = createWSCookie()
      val apiResponse = ApiModel(ApiRequestModel("", "", "", ""), ApiResultModel(results = Json.parse("{}")), Seq(ApiErrorModel("", ERROR_USER_NOT_LOGGED_IN)))
      val cookies: Seq[WSCookie] = Seq(wSCookie)

      when(wsResponse.json).thenReturn(Json.toJson(apiResponse))
      when(wsResponse.cookies).thenReturn(cookies)
      when(wsResponse.status).thenReturn(400)

      val response = httpTransportService.processServiceResponse[UserAccount]("", Some(""), Future.successful(wsResponse))

      whenReady(response.failed) { actualResponse =>
        actualResponse shouldBe a[UnauthorizedException]
      }
    }

    "throw an unauthorized exception if user's session is expired" in {
      val httpTransportService = new HttpTransportService(mock[WSClient], NoOpStatsDClient, ConfigHelper, validator, accountHelper)

      val wsResponse = mock[WSResponse]
      val wSCookie: WSCookie = createWSCookie()
      val apiResponse = ApiModel(ApiRequestModel("", "", "", ""), ApiResultModel(results = Json.parse("{}")), Seq(ApiErrorModel(ERROR_DATA_SESSION_EXPIRED, "")))
      val cookies: Seq[WSCookie] = Seq(wSCookie)

      when(wsResponse.json).thenReturn(Json.toJson(apiResponse))
      when(wsResponse.cookies).thenReturn(cookies)
      when(wsResponse.status).thenReturn(400)

      val response = httpTransportService.processServiceResponse[UserAccount]("", Some(""), Future.successful(wsResponse))

      whenReady(response.failed) { actualResponse =>
        actualResponse shouldBe a[UnauthorizedException]
      }
    }

    "return an generic error response if response state is some other case (unknown error)" in {
      val httpTransportService = new HttpTransportService(mock[WSClient], NoOpStatsDClient, ConfigHelper, validator, accountHelper)

      val wsResponse = mock[WSResponse]

      when(wsResponse.json).thenThrow(new RuntimeException())

      val response = httpTransportService.processServiceResponse[UserAccount]("", Some(""), Future.successful(wsResponse))

      whenReady(response) { actualResponse =>
        actualResponse shouldBe Left(FailureResponse(Seq(ApiErrorModel(GENERIC_ERROR_MESSAGE, GENERIC_ERROR)), 500))
      }
    }

    "return exception message instead of generic message in dev mode" in {
      val configHelper = mock[ConfigHelper]
      when(configHelper.getBooleanProp(DEV_MODE)).thenReturn(true)
      val httpTransportService = new HttpTransportService(mock[WSClient], NoOpStatsDClient, configHelper, validator, accountHelper)

      val wsResponse = mock[WSResponse]

      when(wsResponse.json).thenThrow(new RuntimeException("Exception thrown"))

      val response = httpTransportService.processServiceResponse[UserAccount]("", Some(""), Future.successful(wsResponse))

      whenReady(response) { actualResponse =>
        actualResponse.left.get.code shouldBe 500
        actualResponse.left.get.errors.head.error shouldBe "Exception thrown"
      }
    }
  }
}
