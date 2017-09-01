package unit.model

import models.website.{ ChangePasswordRequest, ResetPasswordRequest }
import org.scalatest._
import play.api.libs.json._

class ChangePasswordRequestSpec extends WordSpec with ShouldMatchers with Matchers {

  "ChangePasswordRequest" should {

    "give error if field patterns are invalid" in {
      val payload = JsObject(List(
        ("user_id", JsString("test#test.com")),
        ("old_password", JsString(" ")),
        ("new_password", JsString("nocaps@1")),
        ("confirm_password", JsString("Short@1"))
      ))

      val validationResults: JsResult[ChangePasswordRequest] = payload.validate[ChangePasswordRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.user_id" =>
                  validationErrors.head.message shouldBe "error.pattern"
                case "obj.old_password" =>
                  validationErrors.head.message shouldBe "error.pattern"
                case "obj.new_password" =>
                  validationErrors.head.message shouldBe "error.pattern"
                case "obj.confirm_password" =>
                  validationErrors.head.message shouldBe "error.pattern"
              }
          }
      }
    }

    "give error if passwords do not match" in {
      val payload = JsObject(List(
        ("user_id", JsString("test@test.com")),
        ("old_password", JsString("Oldpass$1")),
        ("new_password", JsString("Firstpass@2")),
        ("confirm_password", JsString("Different#1"))
      ))

      val validationResults: JsResult[ChangePasswordRequest] = payload.validate[ChangePasswordRequest]

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

    "give successful if passwords do match" in {
      val payload = JsObject(List(
        ("user_id", JsString("test@test.com")),
        ("old_password", JsString("Firstpass%1")),
        ("new_password", JsString("Firstpass#2")),
        ("confirm_password", JsString("Firstpass#2"))
      ))

      val validationResults: JsResult[ChangePasswordRequest] = payload.validate[ChangePasswordRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.new_password should be("Firstpass#2")
          v.confirm_password should be("Firstpass#2")
        case JsError(errors) => fail("Should have passed validate")
      }
    }

    "give successful if passwords is contains special character" in {
      val payload = JsObject(List(
        ("user_id", JsString("test@test.com")),
        ("old_password", JsString(". #@$!(*&^Firstpass1.- ")),
        ("new_password", JsString(". #@$!(*&^Firstpass23.-? ")),
        ("confirm_password", JsString(". #@$!(*&^Firstpass23.-? "))
      ))

      val validationResults: JsResult[ChangePasswordRequest] = payload.validate[ChangePasswordRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.new_password should be((payload \ "new_password").as[String].trim)
          v.confirm_password should be((payload \ "confirm_password").as[String].trim)
        case JsError(errors) => fail("Should have passed validate")
      }
    }
  }
}
