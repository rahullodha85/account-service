package models.website

import play.api.libs.json.Json

trait ResponseModel {
  def header: Seq[Header]
}

case class AddAnotherWebsiteModel(
  messages: Option[Map[String, String]]
)

// Used for swagger only
case class SwaggerMap(item: String, value: String)
case class SwaggerStringBooleanMap(item: String, value: Boolean)

trait AddAnotherWebsiteModelFormats {
  implicit val addAnotherWebsiteModelFormat = Json.format[AddAnotherWebsiteModel]
}
