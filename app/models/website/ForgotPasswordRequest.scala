package models.website

import models.Constants._
import models.FieldConstraints
import play.api.libs.json._

case class ForgotPasswordRequest(
  email: String
)

object ForgotPasswordRequest extends FieldConstraints {
  implicit val forgotPasswordRequestReads: Reads[ForgotPasswordRequest] =
    (__ \ EMAIL).read[String](email).map(ForgotPasswordRequest.apply)
  implicit val forgotPasswordRequestWrites = Json.writes[ForgotPasswordRequest]
}

