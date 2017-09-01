package models.website

import play.api.libs.json._
import models.FieldConstraints
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._ // Combinator syntax

case class SignInUserLabel(
  links:    Option[Map[String, String]],
  messages: Option[Map[String, String]]
)

case class CreateAccountLabel(
  messages: Option[Map[String, String]]
)

case class OrderStatusLabel(
  messages: Map[String, String]
)

case class LockedAccountLabel(
  links:    Option[Map[String, String]],
  messages: Option[Map[String, String]]
)

case class SignInObject(
  sign_in:        SignInUserLabel,
  create_account: CreateAccountLabel,
  order_status:   OrderStatusLabel,
  locked_account: LockedAccountLabel
)

case class CsrUserSignInObject(
  sign_in:        SignInUserLabel,
  order_status:   OrderStatusLabel,
  locked_account: LockedAccountLabel
)

object SignInUserLabel {
  implicit val resultFormat = Json.format[SignInUserLabel]
}

object CreateAccountLabel {
  implicit val resultFormat = Json.format[CreateAccountLabel]
}

object OrderStatusLabel {
  implicit val resultFormat = Json.format[OrderStatusLabel]
}

object LockedAccountLabel {
  implicit val resultFormat = Json.format[LockedAccountLabel]
}

object SignInObject {
  implicit val signInObjectFormat = Json.format[SignInObject]
}

object CsrUserSignInObject {
  implicit val csruserSignInObjectFormat = Json.format[CsrUserSignInObject]
}

sealed trait SignInRequest

case class ClientSignInRequest(username: String, password: String) extends SignInRequest

object ClientSignInRequest extends FieldConstraints {
  implicit val signInReads: Reads[ClientSignInRequest] = (
    (JsPath \ "username").read[String](email).map(_.trim) and
    (JsPath \ "password").read[String](minLength(1))
  )(ClientSignInRequest.apply _)
  implicit val signInWrites = Json.writes[ClientSignInRequest]
}

case class CSRSignInRequest(username: String, site_refer: String) extends SignInRequest

object CSRSignInRequest extends FieldConstraints {
  implicit val signInReads: Reads[CSRSignInRequest] = (
    (JsPath \ "username").read[String](email).map(_.trim) and
    (JsPath \ "site_refer").read[String](minLength(1))
  )(CSRSignInRequest.apply _)
  implicit val signInWrites = Json.writes[CSRSignInRequest]
}
