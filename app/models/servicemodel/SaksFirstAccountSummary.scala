package models.servicemodel

import models.website.Header
import play.api.libs.json.Json

case class SaksFirstAccountSummary(
  enabled:     Boolean,
  member_info: MemberInfo,
  messages:    Option[Map[String, String]],
  links:       Option[Map[String, String]]
)

case class MemberInfo(
  linked:                Boolean,
  loyalty_id:            String,
  available_points:      String,
  tier_status:           String,
  gift_card_amount:      String,
  next_gift_card_amount: String,
  points_to_next_reward: Int,
  redeemable_points:     Int,
  points_multiplier:     Int     = 2,
  rewards_increment:     Int     = 2500
)

case class SaksFirstSummaryPage(
  enabled:            Boolean,
  enabled_redemption: Boolean,
  enabled_beauty:     Boolean,
  member_info:        MemberInfo,
  gift_card_history:  Option[Seq[GiftCardInfo]],
  user_loyalty_info:  Option[UserLoyaltyInfo],
  beauty:             Option[SaksFirstBeauty],
  messages:           Option[Map[String, String]],
  links:              Option[Map[String, String]],
  header:             Option[Seq[Header]]
)

case class GiftCardInfo(
  card_number: String,
  balance:     String
)

case class UserLoyaltyInfo(
  first_name: String,
  last_name:  String,
  line_one:   String,
  line_two:   String,
  city:       String,
  state:      String,
  zip:        String
)

case class SaksFirstBeauty(
  status: String,
  boxes:  Option[Seq[BeautyBox]]
)

case class BeautyBox(
  reward:          String,
  redemption_code: String,
  pin:             String
)

case class BeautyBoxRequest(
  boxes:           Seq[BeautyBox],
  email_addresses: Emails,
  event:           String         = "beauty_box"
)

case class Emails(to: Seq[String], cc: Seq[String], bcc: Seq[String])

object BeautyBoxRequest {
  implicit val emails = Json.format[Emails]
  implicit val beautyBox = Json.format[BeautyBox]
  implicit val beautyBoxEvent = Json.format[BeautyBoxRequest]
  val name = "beauty_box"
}

object SaksFirstSummaryPage {
  implicit val beautyBoxFormat = Json.format[BeautyBox]
  implicit val saksFirstBeautyFormat = Json.format[SaksFirstBeauty]
  implicit val giftCardInfoFormat = Json.format[GiftCardInfo]
  implicit val userLoyaltyInfoFormat = Json.format[UserLoyaltyInfo]
  implicit val memberInfoFormat = Json.format[MemberInfo]
  implicit val pointsInfoFormat = Json.format[SaksFirstSummaryPage]
}

object MemberInfo {
  var empty = MemberInfo(false, "", "", "", "", "", 0, 0)
  implicit val memberInfo = Json.format[MemberInfo]
}

object SaksFirstAccountSummary {
  implicit val memberInfo = Json.format[MemberInfo]
  implicit val saksFirst = Json.format[SaksFirstAccountSummary]

}
