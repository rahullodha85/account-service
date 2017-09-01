package unit.model

import models.servicemodel._
import org.scalatest.WordSpec
import play.api.libs.json.{ JsError, JsResult, JsString, JsSuccess }

class AddressModelSpec extends WordSpec with org.scalatest.Matchers {

  "Addresses model" should {
    "transform self into addresses response model" in {
      val addresses = Addresses(Seq(Address(Option(56297194365387093L), "abc@abc.com", true, "123 main st", Some("apt b"), "nyc", "ny", Some("12345"), "US", "", "first", Some("middle"), "last", "123456789", Some("Saks"))))
      val actualAddressResponse: AddressResponse = addresses.toAddressesResponse.addresses.head

      val expectedAddressesResponse = AddressesResponse(Seq(AddressResponse("56297194365387093", "abc@abc.com", true, "123 main st", Some("apt b"), "nyc", "ny", Some("12345"), "US", "", "first", Some("middle"), "last", "123456789", Some("Saks"))))
      val expectedAddressResponse: AddressResponse = expectedAddressesResponse.addresses.head

      actualAddressResponse.id should be(expectedAddressResponse.id)
      actualAddressResponse.user_id should be(expectedAddressResponse.user_id)
      actualAddressResponse.is_default should be(expectedAddressResponse.is_default)
      actualAddressResponse.address1 should be(expectedAddressResponse.address1)
      actualAddressResponse.address2 should be(expectedAddressResponse.address2)
      actualAddressResponse.city should be(expectedAddressResponse.city)
      actualAddressResponse.state should be(expectedAddressResponse.state)
      actualAddressResponse.zip should be(expectedAddressResponse.zip)
      actualAddressResponse.country should be(expectedAddressResponse.country)
      actualAddressResponse.title should be(expectedAddressResponse.title)
      actualAddressResponse.first_name should be(expectedAddressResponse.first_name)
      actualAddressResponse.middle_name should be(expectedAddressResponse.middle_name)
      actualAddressResponse.last_name should be(expectedAddressResponse.last_name)
      actualAddressResponse.phone should be(expectedAddressResponse.phone)
      actualAddressResponse.company should be(expectedAddressResponse.company)
    }
  }

  "Address type" should {
    "return SHIPPING when transforming from string shipping" in {
      val validationResults: JsResult[AddressType] = JsString("shipping").validate[AddressType]

      validationResults match {
        case JsSuccess(v, _) => v should be(AddressType.SHIPPING)
        case JsError(errors) => fail("Should not have failed to validate")
      }
    }

    "return BILLING when transforming from string billing" in {
      val validationResults: JsResult[AddressType] = JsString("billing").validate[AddressType]

      validationResults match {
        case JsSuccess(v, _) => v should be(AddressType.BILLING)
        case JsError(errors) => fail("Should not have failed to validate")
      }
    }

    "throw unrecognized address type exception when transforming from string that is not shipping or billing" in {
      val validationResults: JsResult[AddressType] = JsString("galactic trade federation").validate[AddressType]

      validationResults match {
        case JsSuccess(v, _) => fail("Should have failed to validate")
        case JsError(errors) => errors should not be empty
      }
    }
  }
}