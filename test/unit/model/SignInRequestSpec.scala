package unit.model

import models.website.{ ClientSignInRequest }
import org.scalatest.{ Matchers, _ }
import play.api.libs.json._

class SignInRequestSpec extends WordSpec with ShouldMatchers with Matchers {

  "SignInRequest" should {
    "return errors if username field is invalid" in {
      val payload = JsObject(List(
        ("username", JsString("abc")),
        ("password", JsString("12345a"))
      ))

      val validationResults: JsResult[ClientSignInRequest] = payload.validate[ClientSignInRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.username" => validationErrors.head.message shouldBe "error.pattern"
              }
          }
      }
    }

    "be successful when email is valid and has spaces" in {
      val payload = JsObject(List(
        ("username", JsString("   abc@email.com   ")),
        ("password", JsString("12345a"))
      ))

      val validationResults: JsResult[ClientSignInRequest] = payload.validate[ClientSignInRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.username should be((payload \ "username").as[String].trim())
          v.password should be((payload \ "password").as[String])
        case JsError(errors) => fail("Should have passed validate")
      }
    }
  }
}
