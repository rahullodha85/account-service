package validations

import org.joda.time.YearMonth

import org.scalatest.{ ShouldMatchers, WordSpec }
import play.api.libs.json.{ JsNull, JsObject, Json }

class FutureOrCurrentYearSpec extends WordSpec with ShouldMatchers {
  "A PaymentMethodExpiryDate" should {

    "be true when brand is SAKS and year is null" in {
      val payload: JsObject = Json.obj("year" -> JsNull, "brand" -> "SAKS")
      FutureOrCurrentYear.process(payload) should be(true)
    }

    "be true when brand is SAKS and any year is specified" in {
      val payload: JsObject = Json.obj("year" -> 2009, "brand" -> "SAKS")
      FutureOrCurrentYear.process(payload) should be(true)
    }

    "be true when the year is in the future and brand is not SAKS" in {
      val payload: JsObject = Json.obj("year" -> (new YearMonth().getYear + 1), "brand" -> "VISA")
      FutureOrCurrentYear.process(payload) should be(true)
    }

    "be false when the year is in the past and brand is not SAKS" in {
      val payload: JsObject = Json.obj("year" -> 2009, "brand" -> "VISA")
      FutureOrCurrentYear.process(payload) should be(false)
    }
  }
}
