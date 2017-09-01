package models.website

import models.{ Constants, FieldConstraints }
import Constants._
import play.api.data.validation.ValidationError
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class LinkSaksFirstRequest(
  name:              String,
  zip:               String,
  saks_first_number: String
)

object LinkSaksFirstRequest extends FieldConstraints {

  implicit val linkSaksFirstAccountRequestReads: Reads[LinkSaksFirstRequest] = (
    (__ \ NAME).read[String](fullName) and
    (__ \ ZIP).read[String](zip) and
    (__ \ CREDIT_CARD_NUMBER).read[String](numbersAndSpaces keepAnd lengthBetween(min = 8, max = 10))
  )(LinkSaksFirstRequest.apply _)

  implicit val linkSaksFirstAccountRequestWrites = Json.writes[LinkSaksFirstRequest]
}

sealed trait RedemptionType { def redemptionType: String }

object RedemptionType {
  case object PHYSICAL_GIFT_CARD extends RedemptionType { val redemptionType = "PHYSICAL_GIFT_CARD" }
  case object ELECTRONIC_GIFT_CARD extends RedemptionType { val redemptionType = "ELECTRONIC_GIFT_CARD" }

  implicit val format = new Format[RedemptionType] {
    def reads(json: JsValue): JsResult[RedemptionType] = json match {
      case JsString(RedemptionType.PHYSICAL_GIFT_CARD.redemptionType) => JsSuccess(RedemptionType.PHYSICAL_GIFT_CARD)
      case JsString(RedemptionType.ELECTRONIC_GIFT_CARD.redemptionType) => JsSuccess(RedemptionType.ELECTRONIC_GIFT_CARD)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("invalid.redemption_type"))))
    }
    def writes(redemptionType: RedemptionType): JsValue = JsString(redemptionType.redemptionType)
  }
}

case class RedemptionRequest(redemption_type: RedemptionType, award_amount: Long)

object RedemptionRequest extends FieldConstraints {
  implicit val redeemGiftCardRequestReads: Reads[RedemptionRequest] = (
    (__ \ "redemption_type").read[RedemptionType] and
    (__ \ "award_amount").read[Long](awardAmount)
  )(RedemptionRequest.apply _)

  implicit val redeemGiftCardRequestWrites = Json.writes[RedemptionRequest]
}