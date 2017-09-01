package unit.model

import models.website.LinkSaksFirstRequest
import org.scalatest.{ Matchers, _ }
import play.api.libs.json._

class LinkSaksFirstRequestSpec extends WordSpec with ShouldMatchers with Matchers {
  "A LinkSaksFirstRequest" should {
    "be invalid given all fields are empty" in {
      val payload = Json.obj(
        "name" -> JsString(""),
        "zip" -> JsString(""),
        "saks_first_number" -> JsString("")
      )

      val validationResults: JsResult[LinkSaksFirstRequest] = payload.validate[LinkSaksFirstRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.name" =>
                  validationErrors.head.message shouldBe "error.pattern"
                case "obj.last_name" =>
                  validationErrors.head.message shouldBe "error.pattern"
                case "obj.zip" =>
                  validationErrors.head.message shouldBe "error.pattern"
                case "obj.saks_first_number" =>
                  validationErrors.head.message shouldBe "error.invalid"
              }
          }
      }
    }

    "be invalid when saks_card_number is longer 10 characters" in {
      val saksFirstRequest = Json.obj("name" -> "george", "zip" -> "66208", "saks_first_number" -> "10000453911")

      saksFirstRequest.validate[LinkSaksFirstRequest] match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.head._1.toJsonString should be("obj.saks_first_number")
          errors.head._2.head.message should be("error.invalid")
      }
    }

    "be invalid when saks_card_number is less than 8 characters" in {
      val saksFirstRequest = Json.obj("name" -> "george", "zip" -> "66208", "saks_first_number" -> "10 00  045")

      saksFirstRequest.validate[LinkSaksFirstRequest] match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.head._1.toJsonString should be("obj.saks_first_number")
          errors.head._2.head.message should be("error.invalid")
      }
    }

    "be valid when saks_card_number is less than 10 characters" in {
      val saksFirstRequest = Json.obj("name" -> "george", "zip" -> "66208", "saks_first_number" -> "10 00  045 00")

      saksFirstRequest.validate[LinkSaksFirstRequest] match {
        case JsSuccess(v, _) =>
        case JsError(errors) => fail("Should have been valid")
      }
    }

    "be valid when saks_card_number is 8 characters" in {
      val saksFirstRequest = Json.obj("name" -> "george", "zip" -> "66208", "saks_first_number" -> "10000450")

      saksFirstRequest.validate[LinkSaksFirstRequest] match {
        case JsSuccess(v, _) =>
        case JsError(errors) => fail("Should have been valid")
      }
    }

    "be valid providing the full name" in {
      val saksFirstRequest = Json.obj("name" -> "Bobby Watkins Hello Dear", "zip" -> "66208", "saks_first_number" -> "10000450")

      saksFirstRequest.validate[LinkSaksFirstRequest] match {
        case JsSuccess(v, _) =>
        case JsError(errors) => fail("Should have been valid")
      }
    }
  }
}
