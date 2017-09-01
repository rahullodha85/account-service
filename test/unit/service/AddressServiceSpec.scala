package unit.service

import constants.Constants._
import fixtures.RequestFixtures
import helpers.{ AccountHelper, ConfigHelper, TogglesHelper }
import models.servicemodel._
import models.website.AddressRequest
import models.{ ApiErrorModel, FailureResponse, SuccessfulResponse }
import org.mockito.Matchers.{ eq => matchEqual }
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }
import play.api.mvc.Cookie
import services._
import utils.NoOpStatsDClient
import utils.TestUtils._

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Right

class AddressServiceSpec extends WordSpec
    with Matchers
    with RequestFixtures
    with MockitoSugar {

  val headers: Map[String, String] = Map("JSESSIONID" -> "123")
  val accountHelper = createIsolatedApplication().build().injector.instanceOf[AccountHelper]

  "Address Service over HTTP " should {
    val address: Address = Address(None, "user@user.user", true, "", None, "city", "state", None, "US", "title", "first", None, "Last", "555-121-2121", None)
    val errorResponse: Future[Left[FailureResponse, Nothing]] = Future.successful(Left(FailureResponse(Seq(ApiErrorModel("Error here", "error.error")), 500)))

    "Get address" should {
      "return with valid addresseses model, cookies, empty errors and 200 has status code for successfull response" in {

        val togglesHelper = mock[TogglesHelper]
        when(togglesHelper.getFavoritesToggleState).thenReturn(Future.successful(false))
        when(togglesHelper.saksFirstPageEnabled).thenReturn(Future.successful(false))

        val httpTransportService = mock[HttpTransportService]

        val addresses = Addresses(Seq(address))
        val successful: Future[Right[Nothing, SuccessfulResponse[Addresses]]] = Future.successful(Right(SuccessfulResponse(addresses, Seq.empty[Cookie])))

        when(httpTransportService.getFromService[Addresses](ConfigHelper.getStringProp("data-service.address"), Some("addresses"), headers, Map(ADDRESS_TYPE -> AddressType.BILLING.addrType))).thenReturn(successful)

        whenReady(new AddressService(httpTransportService, localizationService, ConfigHelper, accountHelper, togglesHelper, NoOpStatsDClient).getAddressBook(headers, AddressType.BILLING, "some/path")) {
          case (addressRes, cookies, errors, code) =>

            addressRes.addresses.size.equals(1) shouldBe true
            cookies.isEmpty shouldBe true
            errors.isEmpty shouldBe true
            code shouldBe 200
        }
      }

      "return with valid addresseses model with enabled false, no cookies , all errors but still 200 has status code for failure response" in {
        val togglesHelper = mock[TogglesHelper]
        when(togglesHelper.getFavoritesToggleState).thenReturn(Future.successful(false))
        when(togglesHelper.saksFirstPageEnabled).thenReturn(Future.successful(false))

        val httpTransportService = mock[HttpTransportService]

        val expectedErrorResponse = errorResponse

        when(httpTransportService.getFromService[Addresses](ConfigHelper.getStringProp("data-service.address"), Some("addresses"), headers, Map(ADDRESS_TYPE -> AddressType.BILLING.addrType)))
          .thenReturn(expectedErrorResponse)

        whenReady(new AddressService(httpTransportService, localizationService, ConfigHelper, accountHelper, togglesHelper, NoOpStatsDClient).getAddressBook(headers, AddressType.BILLING, "some/path")) {
          case (addressRes, cookies, errors, code) =>
            addressRes.enabled shouldBe false
            addressRes.addresses.isEmpty shouldBe true
            cookies.isEmpty shouldBe true
            errors.head.error should be("error.error")
            errors.head.data should be("Error here")
            code shouldBe 200
        }
      }
    }

    "Create address Method" should {

      "return with valid addresseses model, cookies, empty errors and 200 has status code for successful response" in {
        val httpTransportService = mock[HttpTransportService]
        val payload = validAddressRequestPayload.as[AddressRequest]

        val addresses = Addresses(Seq(address))

        when(httpTransportService.postToService[AddressRequest, Addresses](ConfigHelper.getStringProp("data-service.address"), Some("address"), payload, headers, Map(ADDRESS_TYPE -> payload.address_type.addrType)))
          .thenReturn(Future.successful(Right(SuccessfulResponse(addresses, Seq.empty[Cookie]))))

        val addressService = new AddressService(httpTransportService, localizationService, ConfigHelper, accountHelper, mock[TogglesHelper], NoOpStatsDClient)

        whenReady(addressService.createAddress(headers, payload)) {
          case (addressRes, cookies, errors, code) =>
            addressRes.addresses.size.equals(1) shouldBe true
            cookies.isEmpty shouldBe true
            errors.isEmpty shouldBe true
            code shouldBe 200
        }
      }

      "return with valid addresseses model with enabled false, no cookies, all errors but still 400 has status code for failure response" in {
        val httpTransportService = mock[HttpTransportService]

        val payload = validAddressRequestPayload.as[AddressRequest]
        val expectedErrorResponse = errorResponse

        when(httpTransportService.postToService[AddressRequest, Addresses](ConfigHelper.getStringProp("data-service.address"), Some("address"), payload, headers, Map(ADDRESS_TYPE -> payload.address_type.addrType)))
          .thenReturn(expectedErrorResponse)

        val addressService = new AddressService(httpTransportService, localizationService, ConfigHelper, accountHelper, mock[TogglesHelper], NoOpStatsDClient)

        whenReady(addressService.createAddress(headers, payload)) {
          case (addressRes, cookies, errors, code) =>
            addressRes.addresses.isEmpty shouldBe true
            cookies.isEmpty shouldBe true
            errors.head.error should be("error.error")
            errors.head.data should be("Error here")
            code shouldBe 500
        }
      }
    }

    "Update address Method" should {

      "return with valid addresseses model, cookies, empty errors and 200 has status code for successfull response" in {
        val httpTransportService = mock[HttpTransportService]
        val payload = validAddressRequestPayload.as[AddressRequest]
        val addresses = Addresses(Seq(address))

        when(httpTransportService.postToService[AddressRequest, Addresses](ConfigHelper.getStringProp("data-service.address"), Some("address/1234"), payload, headers, Map(ADDRESS_TYPE -> payload.address_type.addrType)))
          .thenReturn(Future.successful(Right(SuccessfulResponse(addresses, Seq.empty[Cookie]))))

        whenReady(new AddressService(httpTransportService, localizationService, ConfigHelper, accountHelper, mock[TogglesHelper], NoOpStatsDClient).updateAddress(headers, "1234", payload)) {
          case (addressRes, cookies, errors, code) =>
            addressRes.addresses.size.equals(1) shouldBe true
            cookies.isEmpty shouldBe true
            errors.isEmpty shouldBe true
            code shouldBe 200
        }
      }

      "return with valid addresseses model with enabled false, no cookies, all errors but still 400 has status code for failure response" in {
        val httpTransportService = mock[HttpTransportService]
        val payload = validAddressRequestPayload.as[AddressRequest]

        val errorResponse: Future[Left[FailureResponse, SuccessfulResponse[Addresses]]] = Future.successful(Left(FailureResponse(Seq(ApiErrorModel("Error here", "error.error")), 500)))

        when(httpTransportService.postToService[AddressRequest, Addresses](ConfigHelper.getStringProp("data-service.address"), Some("address/6789"), payload, headers, Map(ADDRESS_TYPE -> payload.address_type.addrType)))
          .thenReturn(errorResponse)

        val addressService = new AddressService(httpTransportService, localizationService, ConfigHelper, accountHelper, mock[TogglesHelper], NoOpStatsDClient)

        whenReady(addressService.updateAddress(headers, "6789", payload)) {
          case (addressRes, cookies, errors, code) =>
            addressRes.addresses.isEmpty shouldBe true
            cookies.isEmpty shouldBe true
            errors.head.error should be("error.error")
            errors.head.data should be("Error here")
            code shouldBe 500
        }
      }
    }

    "Delete address Method" should {

      "return with valid addresseses model, cookies, empty errors and 200 has status code for successfull response" in {
        val httpTransportService = mock[HttpTransportService]

        val addresses = Addresses(Seq(address))

        when(httpTransportService.deleteFromService[Addresses](ConfigHelper.getStringProp("data-service.address"), Some("address/1234"), headers, Map(ADDRESS_TYPE -> AddressType.BILLING.addrType)))
          .thenReturn(Future.successful(Right(SuccessfulResponse(addresses, Seq.empty[Cookie]))))

        whenReady(new AddressService(httpTransportService, localizationService, ConfigHelper, accountHelper, mock[TogglesHelper], NoOpStatsDClient).deleteAddress(headers, AddressType.BILLING, "1234")) {
          case (addressRes, cookies, errors, code) =>
            addressRes.addresses.size.equals(1) shouldBe true
            cookies.isEmpty shouldBe true
            errors.isEmpty shouldBe true
            code shouldBe 200
        }
      }

      "return with valid addresseses model with enabled false, no cookies, all errors but still 400 has status code for failure response" in {
        val httpTransportService = mock[HttpTransportService]

        val expectedErrorResponse = errorResponse

        when(httpTransportService.deleteFromService[Addresses](ConfigHelper.getStringProp("data-service.address"), Some("address/6789"), headers, Map(ADDRESS_TYPE -> AddressType.BILLING.addrType)))
          .thenReturn(expectedErrorResponse)

        whenReady(new AddressService(httpTransportService, localizationService, ConfigHelper, accountHelper, mock[TogglesHelper], NoOpStatsDClient).deleteAddress(headers, AddressType.BILLING, "6789")) {
          case (addressRes, cookies, errors, code) =>
            addressRes.addresses.isEmpty shouldBe true
            cookies.isEmpty shouldBe true
            errors.head.error should be("error.error")
            errors.head.data should be("Error here")
            code shouldBe 500
        }
      }
    }
  }
}
