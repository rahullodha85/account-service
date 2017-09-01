package models.website

import models.servicemodel.{ UserAccount, UserSignInAuthentication }
import play.api.libs.json.Json

case class SignedInWebsiteModel(
  id:                Option[String],
  email:             Option[String],
  user_id:           String,
  first_name:        String,
  middle_name:       Option[String],
  last_name:         String,
  phone_number:      Option[String],
  saks_anonymous_id: Option[String]     = None,
  messages:          AccountTitleObject
)

object SignedInWebsiteModel {

  def fromUserAccount(user: UserAccount, accountTitle: AccountTitleObject) = {
    SignedInWebsiteModel(user.id, Some(user.email), user.email, user.first_name, user.middle_name, user.last_name, user.phone_number, user.saks_anonymous_id, accountTitle)
  }

  def fromUserAccount(user: UserSignInAuthentication, accountTitle: AccountTitleObject) = {
    SignedInWebsiteModel(user.id, Some(user.email), user.email, user.first_name, user.middle_name, user.last_name, user.phone_number, user.saks_anonymous_id, accountTitle)
  }

  implicit val loginWebsiteModelFormat = Json.format[SignedInWebsiteModel]
}
