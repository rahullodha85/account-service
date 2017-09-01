package models.website

import models.{ Constants, FieldConstraints }
import models.servicemodel.UserAccount
import play.api.libs.json._
import play.api.libs.json.Reads._
import Constants._
import play.api.libs.functional.syntax._

case class UpdateProfileModel(
  id:      String,
  profile: UserAccount
)

object UpdateProfileModel {
  implicit val updateProfileModelFormat = Json.format[UpdateProfileModel]
}

case class UpdateAccountRequest(first_name: String, last_name: String, email: String)

object UpdateAccountRequest extends FieldConstraints {
  implicit val updateAccountReads: Reads[UpdateAccountRequest] = (
    (__ \ FIRST_NAME).read[String](name) and
    (__ \ LAST_NAME).read[String](name) and
    (__ \ EMAIL).read[String](email keepAnd maxLength(250))
  )(UpdateAccountRequest.apply _)

  implicit val updateAccountWrites = Json.writes[UpdateAccountRequest]
}

