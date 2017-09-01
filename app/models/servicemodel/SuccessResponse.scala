package models.servicemodel

import play.api.libs.json.Json

case class SuccessResponse(
  success: Boolean
)

object SuccessResponse {
  implicit val successResponseFormat = Json.format[SuccessResponse]
}

