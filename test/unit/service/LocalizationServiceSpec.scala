package unit.service

import helpers.ConfigHelper
import fixtures.RoutesPath
import mockws.MockWS
import models.servicemodel.{ Country, District }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }
import play.api.libs.json.{ JsValue, _ }
import play.api.mvc.Action
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import services.{ LocalizationClient, LocalizationService }
import utils.NoOpStatsDClient

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Await

class LocalizationServiceSpec extends WordSpec with MockitoSugar
    with Matchers
    with RoutesPath
    with ScalaFutures {

  val localizationServiceEndPoint = ConfigHelper.getStringProp("data-service.localization")
  val internationalCheckoutServiceEndPoint = ConfigHelper.getStringProp("data-service.international-checkout") + "/international_checkout/requirements"

  val countriesEndPoint = localizationServiceEndPoint + "/countries"
  val regionsEndPoint = localizationServiceEndPoint + "/regions"

  val asApiResponse: (Seq[JsValue]) => JsValue = (data: Seq[JsValue]) => Json.obj("response" -> Json.obj("results" -> data))

  val wsFactory: (Seq[JsValue], Seq[JsValue], Seq[JsValue]) => MockWS = (states: Seq[JsValue], countries: Seq[JsValue], req: Seq[JsValue]) => MockWS {
    case (GET, `regionsEndPoint`) => Action {
      Ok(asApiResponse(states))
    }
    case (GET, `countriesEndPoint`) => Action {
      Ok(asApiResponse(countries))
    }
    case (GET, `internationalCheckoutServiceEndPoint`) => Action {
      Ok(asApiResponse(req))
    }
  }

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
  val countries = Seq(
    Json.obj(
      "country_name" -> "Canada",
      "country_code" -> "CA",
      "currency_name" -> "Canadian Dollar",
      "currency_code" -> "CAD",
      "currency_symbol" -> "CAD",
      "shipping_enabled" -> "Y",
      "billing_in_currency" -> "Y"
    )
  )
  val postalCodeRequirements = Seq(
    Json.obj(
      "code" -> "CA",
      "name" -> "Canada",
      "required" -> true
    )
  )

  "LocalizationService" should {

    "throw no such element when countries are empty" in {
      val ws = wsFactory(Seq.empty, Seq.empty, postalCodeRequirements)
      val localizationService = new LocalizationService(new LocalizationClient(ws, ConfigHelper), ConfigHelper, NoOpStatsDClient)
      an[NoSuchElementException] should be thrownBy Await.result(localizationService.cachedCountries("countries"), 5 seconds)
    }

    "return a list of countries" in {
      val ws = wsFactory(canadianProvinces, countries, postalCodeRequirements)
      val localizationService = new LocalizationService(new LocalizationClient(ws, ConfigHelper), ConfigHelper, NoOpStatsDClient)
      val districts: Seq[District] = Seq(District("Alberta", None, "AB"), District("British Columbia", None, "BC"))
      val expected = Country("CA", "Canada", districts, Some(true), postal_code_required = true)

      whenReady(localizationService.cachedCountries("countries")) { countries =>
        countries.find(_._1 == "CA").foreach { canada =>
          canada._2 should be(expected)
        }
      }
    }

    "Shipping enabled is always true for US" in {
      val postalCodeRequirements = Seq(
        Json.obj(
          "code" -> "US",
          "name" -> "United States",
          "required" -> true
        )
      )

      val countries = Seq(
        Json.obj(
          "country_name" -> "United America",
          "country_code" -> "US",
          "currency_name" -> "United States Dollar",
          "currency_code" -> "USD",
          "currency_symbol" -> "USD",
          "shipping_enabled" -> "N",
          "billing_in_currency" -> "Y"
        )
      )

      val states = Seq(Json.obj(
        "display" -> "New York",
        "value" -> "NY"
      ))
      val ws = wsFactory(states, countries, postalCodeRequirements)

      val localizationService = new LocalizationService(new LocalizationClient(ws, ConfigHelper), ConfigHelper, NoOpStatsDClient)
      val districts: Seq[District] = Seq(District("New York", Some(true), "NY"))
      val expected = Country("US", "United States", districts, Some(true), postal_code_required = true)

      whenReady(localizationService.cachedCountries("countries")) { countries =>
        countries.find(_._1 == "US").foreach { us =>
          us._2.code should be(expected.code)
          us._2.can_ship_to shouldBe Some(true)
        }
      }
    }
  }
}
