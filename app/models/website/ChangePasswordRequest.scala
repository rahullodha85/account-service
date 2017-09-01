package models.website

import models.{ Constants, FieldConstraints }
import Constants._
import play.api.data.validation.ValidationError
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class ChangePasswordRequest(
  old_password:     String,
  new_password:     String,
  confirm_password: String
)

object ChangePasswordRequest extends FieldConstraints {

  implicit val changePasswordRequestReads: Reads[ChangePasswordRequest] = (
    (__ \ OLD_PASSWORD).read[String] and //removing the constraint so people with old password format can change their password
    (__ \ NEW_PASSWORD).read[String](password) and
    (__ \ CONFIRM_PASSWORD).read[String](password)
  )(ChangePasswordRequest.apply _).flatMap { changePasswordRequest =>
      Reads { _ =>
        if (changePasswordRequest.new_password == changePasswordRequest.confirm_password) {
          JsSuccess(changePasswordRequest)
        } else {
          JsError(JsPath(List(KeyPathNode("confirm_password"))), ValidationError("passwords.must.match"))
        }
      }
    }

  implicit val changePasswordRequestWrites = Json.writes[ChangePasswordRequest]
}

