package unit.model

import models.website.{ EmailPreferencesRequest, PreferencesRequestModel }
import org.scalatest._
import play.api.libs.json._

class EmailPreferencesRequestModelSpec extends WordSpec with ShouldMatchers with Matchers {
  val headers: Map[String, String] = Map("X-Real-IP" -> "127.0.0.1")
  val userID: String = "abc@abc.com"

  "PreferencesRequestModelSpec" should {
    "returns payload json" in {
      val preferencesRequestModel: PreferencesRequestModel = EmailPreferencesRequest(Seq("saks_opt_status")).toPreferencesRequestModel(headers, userID)

      val payload = Json.toJson(preferencesRequestModel)
      (payload \ "saks_opt_status").as[String] should be("T")
      (payload \ "opt_status").as[String] should be("F")
      (payload \ "saks_canada_opt_status").as[String] should be("F")
      (payload \ "off5th_canada_opt_status").as[String] should be("F")
      (payload \ "ip_address").as[String] should be("127.0.0.1")
    }

    "all be false if preferences are empty" in {
      val preferencesRequestModel: PreferencesRequestModel = EmailPreferencesRequest(Seq()).toPreferencesRequestModel(headers, userID)

      val payload = Json.toJson(preferencesRequestModel)
      (payload \ "saks_opt_status").as[String] should be("F")
      (payload \ "opt_status").as[String] should be("F")
      (payload \ "saks_canada_opt_status").as[String] should be("F")
      (payload \ "off5th_canada_opt_status").as[String] should be("F")
      (payload \ "ip_address").as[String] should be("127.0.0.1")
      (payload \ "email_address").as[String] should be(userID)
    }
  }

  "EmailPreferencesRequest" should {
    "should transform off5th_opt_status into opt_status" in {
      val preferencesRequestModel: PreferencesRequestModel = EmailPreferencesRequest(Seq("off5th_opt_status")).toPreferencesRequestModel(headers, userID)

      val payload = Json.toJson(preferencesRequestModel)
      (payload \ "saks_opt_status").as[String] should be("F")
      (payload \ "opt_status").as[String] should be("T")
      (payload \ "saks_canada_opt_status").as[String] should be("F")
      (payload \ "off5th_canada_opt_status").as[String] should be("F")
      (payload \ "ip_address").as[String] should be("127.0.0.1")
    }

    "give error when preference is invalid" in {
      val request = JsObject(Seq(
        ("preferences", JsArray(Seq(JsString("not in list"), JsString("also not valid")))),
        ("user_id", JsString("abc@email.test"))
      ))

      val result: JsResult[EmailPreferencesRequest] = request.validate[EmailPreferencesRequest]

      result.isError shouldBe true
      result match {
        case JsSuccess(test, path) => fail("Should have failed validation")
        case JsError(errors) => {
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString shouldBe "obj.preferences"
              validationErrors.head.message shouldBe "error.invalid"
          }
        }
      }
    }
  }
}
