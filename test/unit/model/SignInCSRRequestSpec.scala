package unit.model

import models.website.CSRSignInRequest
import org.scalatest.{ Matchers, _ }
import play.api.libs.json._

class SignInCSRRequestSpec extends WordSpec with ShouldMatchers with Matchers {
  "SignInCSRRequest" should {
    "return errors if fields are invalid" in {
      val payload = JsObject(List(
        ("site_refer", JsString("")),
        ("username", JsString("wrong#email.com"))
      ))

      val validationResults: JsResult[CSRSignInRequest] = payload.validate[CSRSignInRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.username"   => validationErrors.head.message shouldBe "error.pattern"
                case "obj.site_refer" => validationErrors.head.message shouldBe "error.minLength"
              }
          }
      }
    }

    "be successful when email is valid" in {
      val payload = JsObject(List(
        ("site_refer", JsString("csr")),
        ("username", JsString("valid@email.com"))
      ))

      val validationResults: JsResult[CSRSignInRequest] = payload.validate[CSRSignInRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.username should be((payload \ "username").as[String])
          v.site_refer should be((payload \ "site_refer").as[String])
        case JsError(errors) => fail("Should have passed validate")
      }
    }

    "be successful when email is valid but has spaces" in {
      val payload = JsObject(List(
        ("site_refer", JsString("csr")),
        ("username", JsString(" valid@email.com "))
      ))

      val validationResults: JsResult[CSRSignInRequest] = payload.validate[CSRSignInRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.username should be((payload \ "username").as[String].trim())
          v.site_refer should be((payload \ "site_refer").as[String])
        case JsError(errors) => fail("Should have passed validate")
      }
    }
  }
}
