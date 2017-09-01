package unit.model

import builders.requests.PaymentMethodPayloadBuilder.PaymentMethodPayloadBuilder
import models.website.{ CreatePaymentMethodRequest, UpdatePaymentMethodRequest }
import org.scalatest._
import play.api.libs.json._

class PaymentMethodRequestSpec extends WordSpec with ShouldMatchers with Matchers {

  "CreatePaymentMethodRequest" should {

    "be valid and trim white spaces in card number" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().build()
      val validationResults: JsResult[CreatePaymentMethodRequest] = payload.validate[CreatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.brand should be((payload \ "brand").as[String])
          v.name should be((payload \ "name").as[String])
          v.number should be("4111111111111")
        case JsError(errors) => fail("Should have succeeded to validate")
      }
    }

    "be valid when month and year are null" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().buildWithNullYearAndMonth()
      val validationResults: JsResult[CreatePaymentMethodRequest] = payload.validate[CreatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.brand should be((payload \ "brand").as[String])
          v.name should be((payload \ "name").as[String])
          v.number should be("4111111111111")
        case JsError(errors) => fail("Should have succeeded to validate")
      }
    }

    "return error when payment brand is missing" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().withPaymentBrand("").build()
      val validationResults: JsResult[CreatePaymentMethodRequest] = payload.validate[CreatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.brand" =>
                  validationErrors.head.message shouldBe "error.invalid"
              }
          }
      }
    }

    "return error when payment name has number" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().withPaymentName("test1 test").build()
      val validationResults: JsResult[CreatePaymentMethodRequest] = payload.validate[CreatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.name" =>
                  validationErrors.head.message shouldBe "error.pattern"
              }
          }
      }
    }

    "return error when payment name has dashes" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().withPaymentName("test-test").build()
      val validationResults: JsResult[CreatePaymentMethodRequest] = payload.validate[CreatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.name" =>
                  validationErrors.head.message shouldBe "error.pattern"
              }
          }
      }
    }

    "return error when payment month is less than 1" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().withPaymentMonth(0).build()
      val validationResults: JsResult[CreatePaymentMethodRequest] = payload.validate[CreatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.month" =>
                  validationErrors.head.message shouldBe "error.min"
              }
          }
      }
    }
    "return error when payment month is greater than 12" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().withPaymentMonth(13).build()
      val validationResults: JsResult[CreatePaymentMethodRequest] = payload.validate[CreatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.month" =>
                  validationErrors.head.message shouldBe "error.max"
              }
          }
      }
    }

    "return error when payment number is contains letters" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().withPaymentNumber("teste1234566").build()
      val validationResults: JsResult[CreatePaymentMethodRequest] = payload.validate[CreatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.number" =>
                  validationErrors.head.message shouldBe "error.pattern"
              }
          }
      }
    }

    "return error when payment brand is invalid" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().withPaymentBrand("AmericanExpressBrand").build()
      val validationResults: JsResult[CreatePaymentMethodRequest] = payload.validate[CreatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.brand" =>
                  validationErrors.head.message shouldBe "error.invalid"
              }
          }
      }
    }
  }

  "UpdatePaymentMethodRequest" should {
    "be valid" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().build()
      val validationResults: JsResult[UpdatePaymentMethodRequest] = payload.validate[UpdatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.name should be((payload \ "name").as[String])
        case JsError(errors) =>
          fail("Should have succeeded to validate")
      }
    }

    "return errors when update payment name is invalid" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().withPaymentName("invalid-name&").build()
      val validationResults: JsResult[UpdatePaymentMethodRequest] = payload.validate[UpdatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.name" =>
                  validationErrors.head.message shouldBe "error.pattern"
              }
          }
      }
    }

    "return errors when update payment name is all spaces" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().withPaymentName("          ").build()
      val validationResults: JsResult[UpdatePaymentMethodRequest] = payload.validate[UpdatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.name" =>
                  validationErrors.head.message shouldBe "error.pattern"
              }
          }
      }
    }

    "return errors when update payment is missing is_default field" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().buildWithIsDefaultMissing()
      val validationResults: JsResult[UpdatePaymentMethodRequest] = payload.validate[UpdatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.is_default" =>
                  validationErrors.head.message shouldBe "error.path.missing"
              }
          }
      }
    }

    "be valid when month and year are null" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().buildWithNullYearAndMonth()
      val validationResults: JsResult[UpdatePaymentMethodRequest] = payload.validate[UpdatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.name should be((payload \ "name").as[String])
        case JsError(errors) => fail("Should have succeeded to validate")
      }
    }

    "return error when payment month is less than 1" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().withPaymentMonth(0).build()
      val validationResults: JsResult[UpdatePaymentMethodRequest] = payload.validate[UpdatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.month" =>
                  validationErrors.head.message shouldBe "error.min"
              }
          }
      }
    }
    "return error when payment month is greater than 12" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().withPaymentMonth(13).build()
      val validationResults: JsResult[UpdatePaymentMethodRequest] = payload.validate[UpdatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.month" =>
                  validationErrors.head.message shouldBe "error.max"
              }
          }
      }

    }

    "be invalid when payment brand is null" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().withPaymentBrand(null).build()
      val validationResults: JsResult[UpdatePaymentMethodRequest] = payload.validate[UpdatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.brand" =>
                  validationErrors.head.message shouldBe "error.invalid"
              }
          }
      }
    }

    "be invalid when payment brand is missing in payload" in {
      val payload: JsObject = new PaymentMethodPayloadBuilder().buildWithMissingBrand()
      val validationResults: JsResult[UpdatePaymentMethodRequest] = payload.validate[UpdatePaymentMethodRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.brand" =>
                  validationErrors.head.message shouldBe "error.path.missing"
              }
          }
      }
    }
  }
}
