package models.website

import models.FailureResponse
import models.servicemodel._
import play.api.libs.json.Json

case class MoreModel(reward_number: String)

// linked: Boolean - this attribute will be removed once new FE goes to PRD
case class SaksFirstModel(linked: Boolean = false, display_rewards: Boolean, member_info: MemberInfo)
case class SaksFirstResponse(enabled: Boolean, member_info: MemberInfo)

case class RewardsWebsiteModel(
  enabled:                Boolean,
  rewards_type:           String,
  rewards_threshold:      Option[Int],
  more_rewards_data:      Option[MoreModel],
  saksfirst_rewards_data: Option[SaksFirstModel],
  links:                  Option[Map[String, String]],
  messages:               Option[Map[String, String]]
)

case class UserAccountWebsiteModel(
    enabled:  Boolean,
    profile:  UserAccount,
    links:    Map[String, String],
    messages: Option[Map[String, String]]
) {
  def copyProfileInfo(phoneNumber: Option[String]) = {
    copy(profile = profile.copy(phone_number = phoneNumber))
  }
}

case class OrdersWebsiteModel(
  enabled:   Boolean,
  orders:    Seq[Order]                  = Seq.empty,
  page_info: PageInfo,
  links:     Option[Map[String, String]],
  messages:  Option[Map[String, String]],
  countries: Map[String, Country]
)

case class OrderDetailsWebsiteModel(
  enabled:   Boolean,
  order:     Option[Order],
  page_info: PageInfo,
  links:     Option[Map[String, String]],
  messages:  Option[Map[String, String]],
  countries: Map[String, Country]
)

case class AccountSummaryResponseModel(
  account_profile: UserAccountWebsiteModel,
  rewards:         RewardsWebsiteModel,
  order_history:   OrdersWebsiteModel,
  header:          Seq[Header],
  messages:        Map[String, String]     = Map.empty
) extends ResponseModel

case class OrdersHistoryWebsiteModel(
  enabled:   Boolean,
  orders:    Seq[Order]                  = Seq.empty,
  header:    Seq[Header],
  page_info: PageInfo,
  links:     Option[Map[String, String]],
  messages:  Option[Map[String, String]],
  countries: Map[String, Country]
)

object OrdersHistoryWebsiteModel extends CountryFormat {
  implicit val pageInfoFormat = Json.format[PageInfo]
  implicit val ordersHistoryWebsiteModel = Json.format[OrdersHistoryWebsiteModel]
}

object MoreModel {
  implicit val moreModel = Json.format[MoreModel]

  def emptyModel = MoreModel("")
}

object SaksFirstModel {
  implicit val memberInfo = Json.format[MemberInfo]
  implicit val saksfirstModel = Json.format[SaksFirstModel]
  def emptyModel = SaksFirstModel
}

object UserAccountWebsiteModel {
  implicit val userAccountWebsiteModel = Json.format[UserAccountWebsiteModel]
}

object RewardsWebsiteModel {
  implicit val rewardsWebsiteModelFormat = Json.format[RewardsWebsiteModel]
}

object SaksFirstResponse {
  implicit val saksFirstResponse = Json.format[SaksFirstResponse]
}

object OrdersWebsiteModel extends CountryFormat {
  implicit val pageInfoFormat = Json.format[PageInfo]
  implicit val ordersWebsiteModelFormat = Json.format[OrdersWebsiteModel]
}

object OrderDetailsWebsiteModel extends CountryFormat {
  implicit val pageInfoFormat = Json.format[PageInfo]
  implicit val orderDetailsWebsiteModelFormat = Json.format[OrderDetailsWebsiteModel]
}

object AccountSummaryResponseModel extends CountryFormat {
  implicit val accountSummaryResponseModelFormat = Json.format[AccountSummaryResponseModel]
}

case class UpdateProfileResponseWebsiteModel(
  profile: UserAccount
)

object UpdateProfileResponseWebsiteModel {
  implicit val updateProfileResponseWebsiteModelFormat = Json.format[UpdateProfileResponseWebsiteModel]
}
