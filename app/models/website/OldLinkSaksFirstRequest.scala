package models.website

import models.{ Constants, FieldConstraints }
import Constants._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class OldLinkSaksFirstRequest(
  first_name:        String,
  last_name:         String,
  zip:               String,
  saks_first_number: String
)

object OldLinkSaksFirstRequest extends FieldConstraints {

  implicit val oldLinkSaksFirstAccountRequestReads: Reads[OldLinkSaksFirstRequest] = (
    (__ \ FIRST_NAME).read[String](name) and
    (__ \ LAST_NAME).read[String](name) and
    (__ \ ZIP).read[String](zip or empty) and
    (__ \ CREDIT_CARD_NUMBER).read[String](numbersAndSpaces keepAnd lengthBetween(min = 8, max = 10))
  )(OldLinkSaksFirstRequest.apply _)

  implicit val oldLinkSaksFirstAccountRequestWrites = Json.writes[OldLinkSaksFirstRequest]
}