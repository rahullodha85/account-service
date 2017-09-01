package models.servicemodel

import play.api.libs.json.Json

case class More(
  more_number:   String,
  email_address: Option[String] = None,
  first_name:    Option[String] = None,
  last_name:     Option[String] = None,
  message:       Option[String] = None,
  phone_number:  Option[String] = None,
  postal_code:   Option[String] = None,
  result_code:   Option[String] = None
)

case class MoreRequest(
  first_name:    Option[String] = None,
  last_name:     String,
  email_address: String,
  phone_number:  String,
  postal_code:   Option[String] = None
)

object More {
  implicit val accountRegistrationModelFormat = Json.format[More]
}

object MoreRequest {
  implicit val moreRequestFormat = Json.format[MoreRequest]
}
