package models.servicemodel

import play.api.libs.json.Json

case class ChangePasswordObject(
  user_id:          String,
  old_password:     String,
  new_password:     String,
  confirm_password: String
)

object ChangePasswordObject {
  implicit val changePasswordObjectFormat = Json.format[ChangePasswordObject]
}

