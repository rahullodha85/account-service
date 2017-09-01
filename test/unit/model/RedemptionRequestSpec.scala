package unit.model

import models.website.RedemptionType.{ ELECTRONIC_GIFT_CARD, PHYSICAL_GIFT_CARD }
import models.website.{ RedemptionRequest, RedemptionType }
import org.scalatest.{ Matchers, WordSpec }
import play.api.libs.json._

class RedemptionRequestSpec extends WordSpec with Matchers {

  "RedeemGiftCardRequest" should {
    "fail if amount is not divisible by 25" in {
      val payload = JsObject(Seq(
        ("redemption_type", JsString("PHYSICAL_GIFT_CARD")),
        ("award_amount", JsNumber(10))
      ))

      val validationResults = payload.validate[RedemptionRequest]

      validationResults match {
        case JsError(errors) =>
          errors.head._1.toJsonString should be("obj.award_amount")
          errors.head._2.head.message should be("error.invalid")
        case _ => fail("Should not have failed to validate")
      }
    }

    "fail if amount is zero" in {
      val payload = JsObject(Seq(
        ("redemption_type", JsString("PHYSICAL_GIFT_CARD")),
        ("award_amount", JsNumber(0))
      ))

      val validationResults = payload.validate[RedemptionRequest]

      validationResults match {
        case JsError(errors) =>
          errors.head._1.toJsonString should be("obj.award_amount")
          errors.head._2.head.message should be("error.invalid")
        case _ => fail("Should not have failed to validate")
      }
    }

    "deserialize if divisible by 25" in {
      val payload = JsObject(Seq(
        ("redemption_type", JsString("PHYSICAL_GIFT_CARD")),
        ("award_amount", JsNumber(50))
      ))

      val validationResults = payload.validate[RedemptionRequest]

      validationResults match {
        case JsSuccess(redeemGiftCardRequest, _) =>
          redeemGiftCardRequest.award_amount should be(50)
        case _ => fail("Should not have failed to validate")
      }
    }
  }

  "RedemptionType" should {

    "deserialize if PHYSICAL_GIFT_CARD" in {
      val payload = JsString("PHYSICAL_GIFT_CARD")

      val validationResults = payload.validate[RedemptionType]

      validationResults match {
        case JsSuccess(redemptionType, _) =>
          redemptionType should be(PHYSICAL_GIFT_CARD)
        case _ => fail("Should not have failed to validate")
      }
    }

    "deserialize if ELECTRONIC_GIFT_CARD" in {
      val payload = JsString("ELECTRONIC_GIFT_CARD")

      val validationResults = payload.validate[RedemptionType]

      validationResults match {
        case JsSuccess(redemptionType, _) =>
          redemptionType should be(ELECTRONIC_GIFT_CARD)
        case _ => fail("Should not have failed to validate")
      }
    }

    "fail to deserialize if unknown type" in {
      val payload = JsString("UNKNOWN")

      val validationResults = payload.validate[RedemptionType]

      validationResults match {
        case JsError(errors) =>
          errors.head._1.toJsonString should be("obj")
          errors.head._2.head.message should be("invalid.redemption_type")
        case _ => fail("Should have failed to validate")
      }
    }
  }
}
