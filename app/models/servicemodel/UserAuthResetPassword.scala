package models.servicemodel

import play.api.libs.json.{ JsPath, Json, Reads }
import play.api.libs.functional.syntax._
import constants.Constants.{ DEFAULT_FIRST_NAME, DEFAULT_LAST_NAME }

case class UserAuthResetPassword(
  first_name:         String,
  last_name:          String,
  email:              String,
  id:                 Option[String],
  is_account_cleared: Boolean
)

object UserAuthResetPassword {
  implicit val userAuthResetPwdReads: Reads[UserAuthResetPassword] = (
    ((JsPath \ "first_name").read[String] or Reads.pure(DEFAULT_FIRST_NAME)) and
    ((JsPath \ "last_name").read[String] or Reads.pure(DEFAULT_LAST_NAME)) and
    (JsPath \ "email").read[String] and
    (JsPath \ "id").readNullable[String] and
    (JsPath \ "is_account_cleared").read[Boolean]
  )(UserAuthResetPassword.apply _)
  implicit val userAuthResetPwdWrites = Json.writes[UserAuthResetPassword]
}
