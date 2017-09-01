package unit.model

import models.website.{ ResetPasswordRequest, ResetPasswordWebsiteModel, CreateAccountRequest }
import org.scalatest._
import play.api.libs.json._

class ResetPasswordWebsiteModelSpec extends WordSpec with ShouldMatchers with Matchers {

  "ResetPasswordWebsiteModel" should {
    "give error if passwords do not match" in {
      val payload = JsObject(List(
        ("password", JsString("First!pass1")),
        ("confirm_password", JsString("difF1!rent"))
      ))

      val validationResults: JsResult[ResetPasswordRequest] = payload.validate[ResetPasswordRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString should be("obj.confirm_password")
              validationErrors.head.message shouldBe "passwords.must.match"
          }
      }
    }

    "give error if passwords min length less than 8" in {
      val payload = JsObject(List(
        ("password", JsString("First!1")),
        ("confirm_password", JsString("Different1!"))
      ))

      val validationResults: JsResult[ResetPasswordRequest] = payload.validate[ResetPasswordRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.password" =>
                  validationErrors.head.message shouldBe "error.pattern"
                case "obj.confirm_password" =>
                  validationErrors.head.message shouldBe "passwords.must.match"
              }
          }
      }
    }

    "give successful if passwords do match" in {
      val payload = JsObject(List(
        ("password", JsString("Firstone1!")),
        ("confirm_password", JsString("Firstone1!"))
      ))

      val validationResults: JsResult[ResetPasswordRequest] = payload.validate[ResetPasswordRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.password should be((payload \ "password").as[String])
          v.confirm_password should be((payload \ "confirm_password").as[String])
        case JsError(errors) => fail("Should have failed to validate")
      }
    }
  }
}
