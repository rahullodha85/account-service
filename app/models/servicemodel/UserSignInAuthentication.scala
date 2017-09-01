package models.servicemodel

import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, Json, Reads }
import constants.Constants.{ DEFAULT_FIRST_NAME, DEFAULT_LAST_NAME }

case class UserSignInAuthentication(
  id:                Option[String],
  email:             String,
  first_name:        String,
  middle_name:       Option[String],
  last_name:         String,
  phone_number:      Option[String],
  saks_anonymous_id: Option[String] = None
)

object UserSignInAuthentication {

  implicit val signInReads: Reads[UserSignInAuthentication] = (
    (JsPath \ "id").readNullable[String] and
    (JsPath \ "email").read[String] and
    ((JsPath \ "first_name").read[String] or Reads.pure(DEFAULT_FIRST_NAME)) and
    (JsPath \ "middle_name").readNullable[String] and
    ((JsPath \ "last_name").read[String] or Reads.pure(DEFAULT_LAST_NAME)) and
    (JsPath \ "phone_number").readNullable[String] and
    (JsPath \ "saks_anonymous_id").readNullable[String]
  )(UserSignInAuthentication.apply _)
  implicit val signInWrites = Json.writes[UserSignInAuthentication]

}
