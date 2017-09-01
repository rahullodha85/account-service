package models.servicemodel

import constants.Constants.{ DEFAULT_FIRST_NAME, DEFAULT_LAST_NAME }
import org.apache.commons.lang3.StringUtils.EMPTY
import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, _ }

case class UserAccount(
  id:                Option[String],
  user_id:           String,
  first_name:        String,
  phone_number:      Option[String],
  email:             String,
  middle_name:       Option[String],
  last_name:         String,
  saks_anonymous_id: Option[String]
)

object UserAccount {
  implicit val userAccountReads: Reads[UserAccount] = (
    (JsPath \ "id").readNullable[String] and
    (JsPath \ "user_id").read[String] and
    ((JsPath \ "first_name").read[String] or Reads.pure(DEFAULT_FIRST_NAME)) and
    (JsPath \ "phone_number").readNullable[String] and
    (JsPath \ "email").read[String] and
    (JsPath \ "middle_name").readNullable[String] and
    ((JsPath \ "last_name").read[String] or Reads.pure(DEFAULT_LAST_NAME)) and
    (JsPath \ "saks_anonymous_id").readNullable[String]
  )(UserAccount.apply _)
  implicit val userAccountWrites = Json.writes[UserAccount]
  def emptyModel = UserAccount(Some(EMPTY), EMPTY, EMPTY, Some(EMPTY), EMPTY, Some(EMPTY), EMPTY, Some(EMPTY))
}
