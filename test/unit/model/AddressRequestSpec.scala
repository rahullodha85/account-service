package unit.model

import builders.requests.AddressRequestBuilder
import com.typesafe.config.ConfigFactory
import models.servicemodel.AddressType
import models.website.AddressRequest
import org.scalatest._
import play.api.libs.json._

class AddressRequestSpec extends WordSpec with ShouldMatchers with Matchers {

  "AddressRequest" should {

    "give error given invalid fields" in {
      val payload = JsObject(List(
        ("user_id", JsString("test#test.com")),
        ("is_default", JsBoolean(true)),
        ("address_type", JsString("nothing")),
        ("address1", JsString("123 test street")),
        ("city", JsString("")),
        ("country", JsString("United States")),
        ("first_name", JsString("first one")),
        ("last_name", JsString("first two")),
        ("company", JsString("Nice co. @33 ave")),
        ("phone", JsString("555wetdgfd"))
      ))

      val validationResults: JsResult[AddressRequest] = payload.validate[AddressRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.user_id" =>
                  validationErrors.head.message shouldBe "error.pattern"
                case "obj.address_type" =>
                  validationErrors.head.message shouldBe "invalid.address.type"
                case "obj.city" =>
                  validationErrors.head.message shouldBe "error.pattern"
                case "obj.first_name" =>
                  validationErrors.head.message shouldBe "error.pattern"
                case "obj.last_name" =>
                  validationErrors.head.message shouldBe "error.pattern"
                case "obj.phone" =>
                  validationErrors.head.message shouldBe "error.pattern"
              }
          }
      }
    }

    "give successful result" in {
      val payload = JsObject(List(
        ("user_id", JsString("test@test.com")),
        ("is_default", JsBoolean(true)),
        ("address_type", JsString("billing")),
        ("address1", JsString("123 test street")),
        ("city", JsString("new york")),
        ("country", JsString("United States")),
        ("first_name", JsString("first1 ")),
        ("last_name", JsString("  first2")),
        ("phone", JsString("+212 333.444.324 "))
      ))

      val validationResults: JsResult[AddressRequest] = payload.validate[AddressRequest]

      validationResults match {
        case JsSuccess(v, _) =>
          v.address_type should be((payload \ "address_type").as[AddressType])
          v.address1 should be((payload \ "address1").as[String])
          v.city should be((payload \ "city").as[String])
          v.first_name should be((payload \ "first_name").as[String].trim)
          v.last_name should be((payload \ "last_name").as[String].trim)
        case JsError(errors) => fail("Should have passed validate")
      }
    }
  }

  ConfigFactory.load()

  "CanadianPostalCode Rule" should {

    "Not validate any postal code that is not Canadian" in {
      val nonCanadianAddress = new AddressRequestBuilder().withCountry("US").withZip("12345").build()

      val validationResults: JsResult[AddressRequest] = nonCanadianAddress.validate[AddressRequest]

      validationResults match {
        case JsSuccess(v, _) => Unit
        case JsError(errors) => fail("Should have passed validate")
      }
    }

    "be invalid if postal code has less than 3 digits" in {
      val nonCanadianAddress = new AddressRequestBuilder().withCountry("US").withZip("12").build()
      val validationResults: JsResult[AddressRequest] = nonCanadianAddress.validate[AddressRequest]

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
      val nonCanadianAddress = new AddressRequestBuilder().withCountry("US").withZip("    12").build()
      val validationResults: JsResult[AddressRequest] = nonCanadianAddress.validate[AddressRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) => errors.foreach {
          case (path, validationErrors) =>
            path.toJsonString should be("obj.zip")
            validationErrors.head.message should be("error.pattern")
        }
      }
    }

    "be invalid if postal code is empty and country is US" in {
      val nonCanadianAddress = new AddressRequestBuilder().withCountry("US").withZip("    ").build()
      val validationResults: JsResult[AddressRequest] = nonCanadianAddress.validate[AddressRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) => errors.foreach {
          case (path, validationErrors) =>
            path.toJsonString should be("obj.zip")
            validationErrors.head.message should be("error.required")
        }
      }
    }

    "be valid if city has accent" in {
      val nonCanadianAddress = new AddressRequestBuilder().withCity("Montréal").build()
      val validationResults: JsResult[AddressRequest] = nonCanadianAddress.validate[AddressRequest]

      validationResults match {
        case JsSuccess(v, _) => Unit
        case JsError(errors) =>
          fail("Should have passed validate")
      }
    }

    "be valid if country has accent" in {
      val nonCanadianAddress = new AddressRequestBuilder().withCountry("Casaquistão").build()
      val validationResults: JsResult[AddressRequest] = nonCanadianAddress.validate[AddressRequest]

      validationResults match {
        case JsSuccess(v, _) => Unit
        case JsError(errors) =>
          fail("Should have failed validate")
      }
    }

    "be invalid if country has * or ;" in {
      val nonCanadianAddress = new AddressRequestBuilder().withCountry("Casaquistão;*").build()
      val validationResults: JsResult[AddressRequest] = nonCanadianAddress.validate[AddressRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have passed validate")
        case JsError(errors) => Unit
      }
    }

    "be invalid if country has --" in {
      val nonCanadianAddress = new AddressRequestBuilder().withCountry("Casaquistão;--").build()
      val validationResults: JsResult[AddressRequest] = nonCanadianAddress.validate[AddressRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed validate")
        case JsError(errors) => Unit
      }
    }

    "be Valid if postal code equals 3 digits" in {
      val nonCanadianAddress = new AddressRequestBuilder().withCountry("US").withZip("123").build()
      val validationResults: JsResult[AddressRequest] = nonCanadianAddress.validate[AddressRequest]

      validationResults match {
        case JsSuccess(v, _) => Unit
        case JsError(errors) => fail("Should have passed validate")
      }
    }

    "Validate that a zip code starting with a T and from AB is valid" in {
      val validCanadianAddress = new AddressRequestBuilder().withCountry("CA").withState("AB").withZip("T2H 0K8").build()

      val validationResults: JsResult[AddressRequest] = validCanadianAddress.validate[AddressRequest]

      validationResults match {
        case JsSuccess(v, _) => Unit
        case JsError(errors) => fail("Should have passed validate")
      }
    }

    "Validate that a zip code not starting with a T and from AB is not valid" in {
      val invalidCanadianAddress = new AddressRequestBuilder().withCountry("CA").withState("AB").withZip("ABC123").build()

      val validationResults: JsResult[AddressRequest] = invalidCanadianAddress.validate[AddressRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.zip" =>
                  validationErrors.head.message shouldBe "canadian_invalid"
              }
          }
      }
    }

    "Validate that a zip code starting with a M and from ON is valid" in {
      val validCanadianAddress = new AddressRequestBuilder().withCountry("CA").withState("ON").withZip("M2H 0K8").build()

      val validationResults: JsResult[AddressRequest] = validCanadianAddress.validate[AddressRequest]

      validationResults match {
        case JsSuccess(v, _) => Unit
        case JsError(errors) => fail("Should have passed validate")
      }
    }

    "Validate that a lower case zip code is still valid" in {
      val validCanadianAddress = new AddressRequestBuilder().withCountry("CA").withState("ON").withZip("m2h 0k8").build()

      val validationResults: JsResult[AddressRequest] = validCanadianAddress.validate[AddressRequest]

      validationResults match {
        case JsSuccess(v, _) => Unit
        case JsError(errors) => fail("Should have passed validate")
      }
    }

    "Validate that a missing zip code is invalid" in {
      val validCanadianAddress = new AddressRequestBuilder().withCountry("CA").withState("ON").withoutZip().build()

      val validationResults: JsResult[AddressRequest] = validCanadianAddress.validate[AddressRequest]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) =>
          errors.foreach {
            case (path, validationErrors) =>
              path.toJsonString match {
                case "obj.zip" =>
                  validationErrors.head.message shouldBe "canadian_invalid"
              }
          }
      }
    }
  }
}

