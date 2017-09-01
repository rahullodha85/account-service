package models.servicemodel

import play.api.libs.json.{ JsPath, Json, Reads }
import play.api.libs.functional.syntax._
import constants.Constants.{ DEFAULT_FIRST_NAME, DEFAULT_LAST_NAME }

case class UserAuthChangePassword(
  first_name:   String,
  phone_number: Option[String],
  email:        String,
  middle_name:  Option[String],
  last_name:    String
)

object UserAuthChangePassword {
  implicit val userAuthChangePasswordReads: Reads[UserAuthChangePassword] = (
    ((JsPath \ "first_name").read[String] or Reads.pure(DEFAULT_FIRST_NAME)) and
    (JsPath \ "phone_number").readNullable[String] and
    (JsPath \ "email").read[String] and
    (JsPath \ "middle_name").readNullable[String] and
    ((JsPath \ "last_name").read[String] or Reads.pure(DEFAULT_LAST_NAME))
  )(UserAuthChangePassword.apply _)
  implicit val userAuthChangePasswordWrites = Json.writes[UserAuthChangePassword]
}

