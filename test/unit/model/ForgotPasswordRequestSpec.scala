package unit.model

import models.website.ForgotPasswordRequest
import org.scalatest.{ Matchers, _ }
import play.api.libs.json._

class ForgotPasswordRequestSpec extends WordSpec with ShouldMatchers with Matchers {
  "ForgotPasswordRequest" should {
    "invalidate when model is invalid" in {
      val payload = JsObject(List(
        ("email", JsString("wrong#email.com"))
      ))

      val validationResults: JsResult[ForgotPasswordRequest] = payload.validate[ForgotPasswordRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString should be("obj.email")
              validationErrors.head.message shouldBe "error.pattern"
          }
      }
    }

    "be successful when email is valid" in {
      val payload = JsObject(List(
        ("email", JsString("valid@email.com"))
      ))

      val validationResults: JsResult[ForgotPasswordRequest] = payload.validate[ForgotPasswordRequest]

      validationResults match {
        case JsSuccess(v, _) => v.email should be((payload \ "email").as[String])
        case JsError(errors) => fail("Should have failed to validate")
      }
    }
  }
}
