package models.servicemodel

import play.api.libs.json.Json

case class CreateAccountModel(
  first_name:       String,
  last_name:        String,
  email:            String,
  password:         String,
  confirm_password: String,
  phone_number:     Option[String]
)

object CreateAccountModel {
  implicit val createAccountModelFormat = Json.format[CreateAccountModel]
}
