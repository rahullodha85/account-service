package validations

import org.joda.time.LocalDate
import org.scalatest.{ ShouldMatchers, WordSpec }
import play.api.libs.json.{ JsNull, JsObject, Json }

class PaymentMethodExpiryDateSpec extends WordSpec with ShouldMatchers {

  "A PaymentMethodExpiryDate" should {

    "be false when month or year is null" in {
      val payload: JsObject = Json.obj("year" -> JsNull, "month" -> JsNull)
      PaymentMethodExpiryDate.process(payload) should be(false)
    }

    "be false when its before today's month" in {
      val now: LocalDate = LocalDate.now()
      val payload: JsObject = Json.obj("year" -> now.minusMonths(1).getYear, "month" -> now.minusMonths(1).getMonthOfYear)

      PaymentMethodExpiryDate.process(payload) should be(false)
    }

    "be true when its after today's month" in {
      val now: LocalDate = LocalDate.now()
      val monthsLeftInTheYear = 12 - now.getMonthOfYear
      val payload: JsObject = Json.obj("year" -> now.getYear, "month" -> (now.getMonthOfYear + monthsLeftInTheYear))

      PaymentMethodExpiryDate.process(payload) should be(true)
    }

    "be true when its equals to today's month" in {
      val now: LocalDate = LocalDate.now()
      val payload: JsObject = Json.obj("year" -> now.getYear, "month" -> now.getMonthOfYear)

      PaymentMethodExpiryDate.process(payload) should be(true)
    }
  }
}
