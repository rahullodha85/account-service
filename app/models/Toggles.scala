package models

import play.api.libs.json._

// java suggests other fields may be possible but I don't know if we care at all
case class Toggle(
  toggle_name:  String,
  toggle_state: Boolean
)

object Toggle {
  implicit val resultFormat = Json.format[Toggle]
}
