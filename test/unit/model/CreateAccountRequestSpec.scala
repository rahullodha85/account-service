package unit.model

import builders.requests.CreateAccountPayloadBuilder
import models.website.CreateAccountRequest
import org.scalatest._
import play.api.libs.json._

class CreateAccountRequestSpec extends WordSpec with ShouldMatchers with Matchers {

  "CreateAccountRequest" should {

    "give error given invalid fields" in {
      val payload = JsObject(List(
        ("first_name", JsString("first one")),
        ("last_name", JsString("first two")),
        ("password", JsString("Firstpass1!")),
        ("confirm_password", JsString("Different1!")),
        ("email", JsString(" test#test.com")),
        ("canadian_customer", JsString("F")),
        ("receive_email", JsBoolean(true)),
        ("canadian_customer_opt_in", JsBoolean(false)),
        ("saks_opt_status", JsString("first")),
        ("off5th_opt_status", JsString("second")),
        ("phone_number", JsString("555 121-212-111"))
      ))

      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.first_name" =>
                  validationErrors.head.message shouldBe "error.pattern"
                case "obj.off5th_opt_status" =>
                  validationErrors.head.message shouldBe "error.invalid"
                case "obj.saks_opt_status" =>
                  validationErrors.head.message shouldBe "error.invalid"
                case "obj.off5th_canada_opt_status" =>
                  validationErrors.head.message shouldBe "error.invalid"
                case "obj.saks_canada_opt_status" =>
                  validationErrors.head.message shouldBe "error.invalid"
                case "obj.last_name" =>
                  validationErrors.head.message shouldBe "error.pattern"
                case "obj.email" =>
                  validationErrors.head.message shouldBe "error.pattern"
                case "obj.confirm_password" =>
                  validationErrors.head.message shouldBe "passwords.must.match"
              }
          }
      }
    }

    "be valid if password has special characters" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withPassword(" $#@! *&^%-_=+?/><;12.test.ing1.':").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be((payload \ "first_name").as[String])
          v.last_name should be((payload \ "last_name").as[String])
          v.email should be((payload \ "email").as[String])
          v.password should be((payload \ "password").as[String])
        case JsError(errors) => fail("Should have succeeded to validate")
      }
    }

    "be valid if phone number is white space" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withPhoneNumber(" ").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be((payload \ "first_name").as[String])
          v.last_name should be((payload \ "last_name").as[String])
          v.email should be((payload \ "email").as[String])
        case JsError(errors) => fail("Should have succeeded to validate")
      }
    }

    "be valid if phone number is empty" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withPhoneNumber("").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be((payload \ "first_name").as[String])
          v.last_name should be((payload \ "last_name").as[String])
          v.email should be((payload \ "email").as[String])
        case JsError(errors) => fail("Should have succeeded to validate")
      }
    }

    "be valid if phone number has spaces between digits" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withPhoneNumber("222 222 111 121").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be((payload \ "first_name").as[String])
          v.last_name should be((payload \ "last_name").as[String])
          v.email should be((payload \ "email").as[String])
        case JsError(errors) => fail("Should have succeeded to validate")
      }
    }

    "be valid if phone number has spaces periods between digits" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withPhoneNumber("222.222.111.121").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be((payload \ "first_name").as[String])
          v.last_name should be((payload \ "last_name").as[String])
          v.email should be((payload \ "email").as[String])
        case JsError(errors) => fail("Should have succeeded to validate")
      }
    }

    "be valid if phone number starts with +" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withPhoneNumber("+1 222.111.121").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be((payload \ "first_name").as[String])
          v.last_name should be((payload \ "last_name").as[String])
          v.email should be((payload \ "email").as[String])
        case JsError(errors) => fail("Should have succeeded to validate")
      }
    }

    "be invalid if phone number has less than 7 digits" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withPhoneNumber("1234").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) => errors.foreach {
          case (path, validationErrors) =>
            path.toJsonString should be("obj.phone_number")
            validationErrors.head.message should be("error.pattern")
        }
      }
    }

    "be invalid if phone number has + anywhere except the begining" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withPhoneNumber("323231234+32").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) => errors.foreach {
          case (path, validationErrors) =>
            path.toJsonString should be("obj.phone_number")
            validationErrors.head.message should be("error.pattern")
        }
      }
    }

    "be valid if postal code is white space" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withPostalCode(" ").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be((payload \ "first_name").as[String])
          v.last_name should be((payload \ "last_name").as[String])
          v.email should be((payload \ "email").as[String])
        case JsError(errors) => fail("Should have succeeded to validate")
      }
    }

    "be valid if postal code is empty" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withPostalCode("").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be((payload \ "first_name").as[String])
          v.last_name should be((payload \ "last_name").as[String])
          v.email should be((payload \ "email").as[String])
        case JsError(errors) => fail("Should have succeeded to validate")
      }
    }

    "be valid if postal code has spaces" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withPostalCode("abc dec ").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be((payload \ "first_name").as[String])
          v.last_name should be((payload \ "last_name").as[String])
          v.email should be((payload \ "email").as[String])
          v.zip should be((payload \ "zip").asOpt[String].map(_.trim))
        case JsError(errors) => fail("Should have succeeded to validate")
      }
    }

    "be valid if email has spaces in begin and end" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withEmail(" abc@abc.com ").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be((payload \ "first_name").as[String])
          v.last_name should be((payload \ "last_name").as[String])
          v.email should be((payload \ "email").as[String].trim)
        case JsError(errors) => fail("Should have succeeded to validate")
      }
    }

    "be invalid if postal code has less than 3 digits" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withPostalCode("12").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) => errors.foreach {
          case (path, validationErrors) =>
            path.toJsonString should be("obj.zip")
            validationErrors.head.message should be("error.pattern")
        }
      }
    }

    "be invalid if postal code has less than 3 digits with extra spaces" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withPostalCode("      12").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) => errors.foreach {
          case (path, validationErrors) =>
            path.toJsonString should be("obj.zip")
            validationErrors.head.message should be("error.pattern")
        }
      }
    }

    "be valid if postal code equals 3 digits" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withPostalCode("T4A").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be((payload \ "first_name").as[String])
          v.last_name should be((payload \ "last_name").as[String])
          v.email should be((payload \ "email").as[String].trim)
          v.zip should be((payload \ "zip").asOpt[String])
        case JsError(errors) => fail("Should have succeeded to validate")
      }
    }

    "be invalid firstname or lastname are empty with spaces" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withFirstName("       ").withLastName("      ").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) => errors.foreach {
          case (path, validationErrors) =>
            path.toJsonString match {
              case "obj.last_name" =>
                validationErrors.head.message should be("error.pattern")
              case "obj.first_name" =>
                validationErrors.head.message should be("error.pattern")
            }
        }
      }
    }

    "be valid when name has accents" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withFirstName("Renée").withLastName("Sørina").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be((payload \ "first_name").as[String])
        case JsError(errors) =>
          fail("Should have validated")
      }
    }

    "give successful result" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be((payload \ "first_name").as[String])
          v.last_name should be((payload \ "last_name").as[String])
          v.email should be((payload \ "email").as[String])
        case JsError(errors) => fail("Should have succeeded to validate")
      }
    }

    "give successful result when email has dashes within dots after @ sign" in {
      val payload: JsObject = new CreateAccountPayloadBuilder().withEmail("test@test.-.com").buildJson()
      val validationResults: JsResult[CreateAccountRequest] = payload.validate[CreateAccountRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.first_name should be((payload \ "first_name").as[String])
          v.last_name should be((payload \ "last_name").as[String])
          v.email should be((payload \ "email").as[String])
        case JsError(errors) => fail("Should have succeeded to validate")
      }
    }
  }
}
