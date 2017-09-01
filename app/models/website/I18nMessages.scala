package models.website

import play.api.libs.json.Json
case class Header(
  id:     String,
  active: Boolean,
  label:  String
)

case class Message(
  header: Seq[Header]
)
object Header {
  implicit val headerFormat = Json.format[Header]
}

object Message {
  implicit val headerFormat = Json.format[Message]
}
