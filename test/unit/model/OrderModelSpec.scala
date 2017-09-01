package unit.model

import models.servicemodel._
import org.scalatest.WordSpec
import play.api.libs.json._

class OrderModelSpec extends WordSpec with org.scalatest.Matchers {

  "ItemShippingDetails model" should {
    "strip spaces for the shipping date so that front end logic to show 'not available' works" in {
      val json = JsObject(List(
        ("extended_shipping_charge", JsNumber(0)),
        ("display_extended_shipping_charge", JsString("")),
        ("estimated_delivery_date", JsString(" ")),
        ("signature_required", JsBoolean(true)),
        ("shipping_method", JsString("")),
        ("extended_shipping_tax", JsNumber(0)),
        ("display_extended_shipping_tax", JsString("")),
        ("tracking_numbers", JsArray()),
        ("tracking_links", JsArray())
      ))

      val validationResults: JsResult[ItemShippingDetails] = json.validate[ItemShippingDetails]

      validationResults match {
        case JsSuccess(v, _) =>
          v.estimated_delivery_date should be("")
        case JsError(errors) => fail("Should not have failed deserialization")
      }
    }
  }
}
