package unit.model

import constants.Constants._
import models.servicemodel._
import org.scalatest.WordSpec
import play.api.libs.json.{ JsBoolean, _ }

class NullNameSpec extends WordSpec with org.scalatest.Matchers {

  "In case of null first and last name these models" should {

    "UserAccount deserialize properly" in {
      val payload = JsObject(List(
        ("user_id", JsString("test@test.com")),
        ("email", JsString("")),
        ("is_saksfirst", JsBoolean(false))
      ))

      val validationResults = payload.validate[UserAccount]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be(DEFAULT_FIRST_NAME)
          v.last_name should be(DEFAULT_LAST_NAME)
        case JsError(errors) => fail("Should not have failed to validate")
      }
    }

    "UserAuthChangePassword deserialize properly" in {
      val payload = JsObject(List(
        ("user_id", JsString("test@test.com")),
        ("email", JsString(""))
      ))

      val validationResults = payload.validate[UserAuthChangePassword]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be(DEFAULT_FIRST_NAME)
          v.last_name should be(DEFAULT_LAST_NAME)
        case JsError(errors) => fail("Should not have failed to validate")
      }
    }

    "UserAuthResetPassword deserialize properly" in {
      val payload = JsObject(List(
        ("email", JsString("")),
        ("id", JsString("")),
        ("is_account_cleared", JsBoolean(false))
      ))

      val validationResults = payload.validate[UserAuthResetPassword]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be(DEFAULT_FIRST_NAME)
          v.last_name should be(DEFAULT_LAST_NAME)
        case JsError(errors) => fail("Should not have failed to validate")
      }
    }

    "UserSignInAuthentication deserialize properly" in {
      val payload = JsObject(List(
        ("email", JsString(""))
      ))

      val validationResults = payload.validate[UserSignInAuthentication]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be(DEFAULT_FIRST_NAME)
          v.last_name should be(DEFAULT_LAST_NAME)
        case JsError(errors) => fail("Should not have failed to validate")
      }
    }
  }
}