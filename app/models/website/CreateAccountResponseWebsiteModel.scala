package models.website

import models.servicemodel.UserAccount
import play.api.libs.json.Json

case class AccountTitleObject(
  account_title:            String,
  blank_name_account_title: String
)

case class CreateAccountResponseWebsiteModel(
  id:          Option[String],
  user_id:     String,
  first_name:  String,
  last_name:   String,
  middle_name: Option[String]     = Some(""),
  email:       String,
  messages:    AccountTitleObject
)

object AccountTitleObject {
  implicit val accountTitleObjectFormat = Json.format[AccountTitleObject]
}

object CreateAccountResponseWebsiteModel {

  def fromUserAccount(userAccount: UserAccount, accountTitleObject: AccountTitleObject) = {
    CreateAccountResponseWebsiteModel(userAccount.id, userAccount.email, userAccount.first_name, userAccount.last_name, userAccount.middle_name, userAccount.email, accountTitleObject)
  }

  implicit val createAccountResponseWebsiteModelFormat = Json.format[CreateAccountResponseWebsiteModel]
}
