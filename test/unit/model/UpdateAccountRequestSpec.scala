package unit.model

import models.website.UpdateAccountRequest
import org.scalatest._
import play.api.libs.json._

class UpdateAccountRequestSpec extends WordSpec with ShouldMatchers with Matchers {

  "UpdateAccountRequest" should {

    "give error given invalid fields" in {
      val payload = JsObject(List(
        ("first_name", JsString("first one")),
        ("last_name", JsString("first two")),
        ("email", JsString("not a valid email"))
      ))

      val validationResults: JsResult[UpdateAccountRequest] = payload.validate[UpdateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.first_name" =>
                  validationErrors.head.message shouldBe "error.pattern"
                case "obj.last_name" =>
                  validationErrors.head.message shouldBe "error.pattern"
                case "obj.email" =>
                  validationErrors.head.message shouldBe "error.pattern"
                case "obj.user_id" =>
                  validationErrors.head.message shouldBe "error.path.missing"
              }
          }
      }
    }

    "be valid given correct values" in {
      val payload = JsObject(List(
        ("first_name", JsString("somename")),
        ("last_name", JsString("someothername")),
        ("email", JsString(" valid@email.com ")),
        ("user_id", JsString("valid@email.com"))
      ))

      val validationResults: JsResult[UpdateAccountRequest] = payload.validate[UpdateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be((payload \ "first_name").as[String])
          v.last_name should be((payload \ "last_name").as[String])
          v.email should be((payload \ "email").as[String].trim)
        case JsError(errors) => fail("Should have succeeded to validate")
      }
    }
  }
}
