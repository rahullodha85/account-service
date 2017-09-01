package models.website

import models.{ Constants, FieldConstraints }
import Constants._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class CreatePaymentMethodRequest(
  is_default: Boolean,
  brand:      String,
  name:       String,
  number:     String,
  month:      Option[Int],
  year:       Option[Int],
  zip:        Option[String]
)

object CreatePaymentMethodRequest extends FieldConstraints {

  implicit val paymentMethodRequestReads: Reads[CreatePaymentMethodRequest] = (
    (__ \ IS_DEFAULT_PAYMENT).read[Boolean] and
    (__ \ PAYMENT_BRAND).read[String](validBrand) and
    (__ \ PAYMENT_NAME).read[String](fullName) and
    (__ \ PAYMENT_NUMBER).read[String](numbersAndSpaces keepAnd (minLength(1) andKeep maxLength(40))).map(_.replaceAll("\\s", "")) and
    (__ \ PAYMENT_MONTH).readNullable[Int](min(1) keepAnd max(12)) and
    (__ \ PAYMENT_YEAR).readNullable[Int] and
    (__ \ ZIP).readNullable[String](zip)
  )(CreatePaymentMethodRequest.apply _)

  implicit val paymentMethodRequestWrites = Json.writes[CreatePaymentMethodRequest]
}

case class UpdatePaymentMethodRequest(
  id:         Long,
  is_default: Boolean,
  name:       String,
  brand:      String,
  month:      Option[Int],
  year:       Option[Int]
)

object UpdatePaymentMethodRequest extends FieldConstraints {

  implicit val updatePaymentMethodRequestReads: Reads[UpdatePaymentMethodRequest] = (
    (__ \ PAYMENT_ID).read[Long](min(1L)) and
    (__ \ IS_DEFAULT_PAYMENT).read[Boolean] and
    (__ \ PAYMENT_NAME).read[String](fullName) and
    (__ \ PAYMENT_BRAND).read[String](validBrand) and
    (__ \ PAYMENT_MONTH).readNullable[Int](min(1) keepAnd max(12)) and
    (__ \ PAYMENT_YEAR).readNullable[Int]
  )(UpdatePaymentMethodRequest.apply _)

  implicit val updatePaymentMethodRequestWrites = Json.writes[UpdatePaymentMethodRequest]
}
