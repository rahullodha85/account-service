package models.website

import models.{ Constants, FieldConstraints }
import play.api.data.validation.ValidationError
import play.api.libs.json.Reads._
import play.api.libs.json._
import Constants._
import play.api.libs.functional.syntax._

case class ResetPasswordTitleObject(
  account_title:           String,
  account_cleared_message: String
)

case class ResetPasswordWebsiteModel(
  id:                 Option[String],
  user_id:            String,
  first_name:         String,
  last_name:          String,
  is_account_cleared: Boolean,
  messages:           ResetPasswordTitleObject
)

object ResetPasswordTitleObject {
  implicit val resetPasswordTitleObjectFormat = Json.format[ResetPasswordTitleObject]
}

object ResetPasswordWebsiteModel {
  implicit val resetPasswordWebsiteModelFormat = Json.format[ResetPasswordWebsiteModel]
}

case class ResetPasswordRequest(
  password:         String,
  confirm_password: String
)

object ResetPasswordRequest extends FieldConstraints {

  implicit val resetPasswordRequestReads: Reads[ResetPasswordRequest] = (
    (__ \ PASSWORD).read[String](password) and
    (__ \ CONFIRM_PASSWORD).read[String](password)
  )(ResetPasswordRequest.apply _).flatMap { resetPasswordRequest =>
      Reads { _ =>
        if (resetPasswordRequest.password == resetPasswordRequest.confirm_password) {
          JsSuccess(resetPasswordRequest)
        } else {
          JsError(JsPath(List(KeyPathNode("confirm_password"))), ValidationError("passwords.must.match"))
        }
      }
    }

  implicit val resetPasswordRequestWrites = Json.writes[ResetPasswordRequest]
}
