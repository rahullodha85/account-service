package models.servicemodel

import play.api.libs.json.Json

case class PaymentMethodModel(
  brand:         String,
  id:            Int,
  is_default:    Boolean,
  month:         Int,
  name:          String,
  number:        String,
  security_code: String,
  user_id:       String,
  empty:         Boolean,
  was_validated: Boolean,
  year:          Int
)

case class PaymentMethodsModel(
  credit_cards: Seq[PaymentMethodModel]
)

object PaymentMethodModel {
  implicit val paymentMethodModelFormat = Json.format[PaymentMethodModel]
}

object PaymentMethodsModel {
  implicit val paymentMethodsModelFormat = Json.format[PaymentMethodsModel]
}

