package builders.requests

import play.api.libs.json.{ JsNull, Json }

object PaymentMethodPayloadBuilder {

  class PaymentMethodPayloadBuilder {
    val user_id: String = "csmith@obs1.com"
    var is_default: Boolean = false
    var brand: String = "VISA"
    var name: String = "test test"
    var number: String = "4111 1111 11111 "
    var month: Int = 1
    var year: Int = 2017
    var id: Int = 1

    def build() = {
      Json.obj(
        "id" -> id,
        "user_id" -> user_id,
        "is_default" -> is_default,
        "brand" -> brand,
        "name" -> name,
        "number" -> number,
        "month" -> month,
        "year" -> year
      )
    }

    def buildWithIsDefaultMissing() = {
      Json.obj(
        "id" -> id,
        "user_id" -> user_id,
        "brand" -> brand,
        "name" -> name,
        "number" -> number,
        "month" -> month,
        "year" -> year
      )
    }

    def buildWithMissingBrand() = {
      Json.obj(
        "id" -> id,
        "user_id" -> user_id,
        "is_default" -> is_default,
        "name" -> name,
        "number" -> number,
        "month" -> month,
        "year" -> year
      )
    }

    def buildWithNullYearAndMonth() = {
      Json.obj(
        "id" -> id,
        "user_id" -> user_id,
        "is_default" -> is_default,
        "brand" -> brand,
        "name" -> name,
        "number" -> number,
        "month" -> JsNull,
        "year" -> JsNull
      )
    }

    def withPaymentBrand(paymentBrand: String): PaymentMethodPayloadBuilder = {
      this.brand = paymentBrand
      this
    }

    def withPaymentName(paymentName: String): PaymentMethodPayloadBuilder = {
      this.name = paymentName
      this
    }

    def withPaymentNumber(number: String): PaymentMethodPayloadBuilder = {
      this.number = number
      this
    }

    def withPaymentMonth(month: Int): PaymentMethodPayloadBuilder = {
      this.month = month
      this
    }

    def withPaymentYear(year: Int): PaymentMethodPayloadBuilder = {
      this.year = year
      this
    }

    def missingIsDefaultField: PaymentMethodPayloadBuilder = {
      this.is_default = false
      this
    }
  }

}

