package unit.service

import helpers.ConfigHelper
import fixtures.RoutesPath
import mockws.MockWS
import models.servicemodel.Localization.Region
import models.servicemodel.LocalizationValidations
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }
import play.api.libs.json._
import play.api.libs.ws.{ WSClient, WSRequest }
import play.api.mvc.Action
import play.api.mvc.Results.{ NotFound, Ok }
import play.api.test.Helpers._
import services.{ ConnectionException, LocalizationClient, ServiceException }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.language.postfixOps

class LocalizationClientSpec extends WordSpec with MockitoSugar
    with Matchers
    with RoutesPath
    with ScalaFutures
    with LocalizationValidations {

  val localizationServiceEndPoint = ConfigHelper.getStringProp("localization-service")
  val regionsEndPoint = localizationServiceEndPoint + "/regions"
  val asApiResponse: (Seq[JsValue]) => JsValue = (data: Seq[JsValue]) => Json.obj("response" -> Json.obj("results" -> data))

  val ws = MockWS {
    case (GET, `regionsEndPoint`) => Action {
      val canadianProvinces = Seq(
        Json.obj(
          "display" -> "Alberta",
          "value" -> "AB"
        ),
        Json.obj(
          "display" -> "British Columbia",
          "value" -> "BC"
        )
      )
      Ok(asApiResponse(canadianProvinces))
    }
  }

  "LocalizationService" should {
    "return a list of countries" in {
      val localizationClient = new LocalizationClient(ws, ConfigHelper)

      val regions = Await.result(localizationClient.get[Region](regionsEndPoint, validateRegions), 10 seconds)
      regions.size should be(2)
      regions.head should be(Region("Alberta", "AB"))
      regions.last should be(Region("British Columbia", "BC"))
    }
  }

  "throw ServiceError when endpoint returns a non 200 status code" in {
    val ws = MockWS {
      case (GET, `regionsEndPoint`) => Action {
        NotFound
      }
    }

    val localizationClient = new LocalizationClient(ws, ConfigHelper)

    an[ServiceException] should be thrownBy Await.result(localizationClient.get[Region](regionsEndPoint, validateRegions), 10 seconds)
  }

  "throw ConnectionException when service unreachable" in {
    val ws = mock[WSClient]
    val request = mock[WSRequest]

    when(ws.url(any[String])).thenReturn(request)
    when(request.withRequestTimeout(any())).thenReturn(request)
    when(request.withHeaders(any[(String, String)])).thenReturn(request)
    when(request.get()).thenReturn(Future.failed(new java.net.ConnectException("unable to connect")))

    val localizationClient = new LocalizationClient(ws, ConfigHelper)

    an[ConnectionException] should be thrownBy Await.result(localizationClient.get[Region](regionsEndPoint, validateRegions), 10 seconds)
  }
}
