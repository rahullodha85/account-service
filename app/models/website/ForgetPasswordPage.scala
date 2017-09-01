package models.website

import play.api.libs.json.Json

case class ForgotPasswordLabel(
  links:          Map[String, String],
  messages:       Option[Map[String, String]],
  locked_account: LockedAccountLabel
)

case class ResetPasswordLabel(
  messages: Option[Map[String, String]],
  links:    Map[String, String]
)

object ForgotPasswordLabel {
  implicit val forgotPasswordLabelFormat = Json.format[ForgotPasswordLabel]
}

object ResetPasswordLabel {
  implicit val passwordResetLabelFormat = Json.format[ResetPasswordLabel]
}
