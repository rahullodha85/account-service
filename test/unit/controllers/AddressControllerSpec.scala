package unit.controllers

import builders.requests.AddressRequestBuilder
import fixtures.{ RequestFixtures, RoutesPath }
import models.ApiErrorModel
import models.servicemodel._
import models.website.{ AddressRequest, AddressWebsiteModel }
import org.mockito.Matchers.{ any, eq => matchEqual }
import org.mockito.Mockito.{ mock => _, _ }
import org.scalatest.{ BeforeAndAfterEach, Matchers, WordSpec }
import play.api.inject._
import play.api.mvc.{ Cookie, Result }
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AddressService
import utils.TestUtils._

import scala.concurrent.Future
import scala.language.postfixOps

class AddressControllerSpec extends WordSpec
    with Matchers
    with BeforeAndAfterEach
    with RequestFixtures
    with RoutesPath {

  val addressService = mock[AddressService]
  val application = createIsolatedApplication()
    .overrides(bind[AddressService].toInstance(addressService))
    .build()

  override def beforeEach(): Unit = {
    reset(addressService)
  }

  "Address Controller" should {

    "call address service with path when getting all addresses" in {
      val eventualResult: Future[Result] = route(application, FakeRequest(GET, versionCtx + "/account-service/accounts/123/addresses?address_type=shipping").withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get

      verify(addressService, timeout(1000)).getAddressBook(any[Map[String, String]], matchEqual(AddressType.SHIPPING), matchEqual("/v1/account-service/accounts/123/addresses"))
    }

    "get addresses action" should {
      "return with a status of " + UNAUTHORIZED + "  due to the absence of a \"JSESSIONID\" Cookie" in {
        val result = route(application, FakeRequest(GET, versionCtx + route_getAddressBook)).get
        status(result) shouldBe UNAUTHORIZED
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + OK + " when valid cookies are present" in {
        val successfulAddressResponse = Future.successful((AddressWebsiteModel(true, Seq.empty, Seq.empty, None, None, Map.empty), Seq.empty, Seq.empty, 200))
        when(addressService.getAddressBook(any[Map[String, String]], any(), any())).thenReturn(successfulAddressResponse)

        val result = route(application, FakeRequest(GET, versionCtx + route_getAddressBook).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "response" \ "results" \ "addresses").as[Array[Addresses]].length.equals(0) shouldBe true
        header("cache-control", result).get shouldBe "max-age=0, no-cache"
      }

      "return with a status of " + BAD_REQUEST + " when bad address type queried" in {
        val result = route(application, FakeRequest(GET, versionCtx + "/account-service/accounts/123/addresses?address_type=GARBAGE").withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].length shouldBe 1
      }
    }

    "create addresses action" should {
      "return with a status of " + UNAUTHORIZED + "  due to the absence of a \"JSESSIONID\" Cookie" in {
        val result = route(application, FakeRequest(POST, versionCtx + route_createAddress)).get
        status(result) shouldBe UNAUTHORIZED
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + OK + " when valid cookies are present" in {
        val successfulAddressResponse = Future.successful((AddressesResponse(Seq.empty), Seq.empty, Seq.empty, 200))
        when(addressService.createAddress(any[Map[String, String]], any[AddressRequest])).thenReturn(successfulAddressResponse)

        val result = route(application, FakeRequest(POST, versionCtx + route_createAddress).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc")).withJsonBody(validAddressRequestPayload)).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "response" \ "results" \ "addresses").as[Array[Addresses]].length.equals(0) shouldBe true
      }

      "return with a status of " + BAD_REQUEST + " when payload with missing required fields" in {
        val result = route(application, FakeRequest(POST, versionCtx + route_createAddress).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc")).withJsonBody(invalidCreateAddressPayloadMissingFields)).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].length shouldBe 2
      }

      "return with a status of " + OK + " when valid cookies with missing optional fields" in {
        val successfulAddressResponse = Future.successful((AddressesResponse(Seq.empty), Seq.empty, Seq.empty, 200))
        when(addressService.createAddress(any[Map[String, String]], any[AddressRequest])).thenReturn(successfulAddressResponse)

        val result = route(application, FakeRequest(POST, versionCtx + route_createAddress).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc")).withJsonBody(validCreateAddressPayloadMissingOpt)).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "response" \ "results" \ "addresses").as[Seq[Addresses]].length.equals(0) shouldBe true
      }

      "return with a status of " + BAD_REQUEST + " when bad address type entered" in {
        val badAddress = new AddressRequestBuilder().withAddressType("GARBAGE").build()

        val result = route(application, FakeRequest(POST, versionCtx + route_createAddress).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc")).withJsonBody(badAddress)).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].length shouldBe 1
      }
    }

    "update addresses action" should {
      "return with a status of " + UNAUTHORIZED + "  due to the absence of a \"JSESSIONID\" Cookie" in {
        val result = route(application, FakeRequest(PUT, versionCtx + route_updateDeleteAddress)).get
        status(result) shouldBe UNAUTHORIZED
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + OK + " when valid cookies are present" in {
        val successfulAddressResponse = Future.successful((AddressesResponse(Seq.empty), Seq.empty, Seq.empty, 200))
        when(addressService.updateAddress(any[Map[String, String]], any[String], any[AddressRequest])).thenReturn(successfulAddressResponse)

        val result = route(application, FakeRequest(PUT, versionCtx + route_updateDeleteAddress).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc")).withJsonBody(validCreateAddressPayloadMissingOpt)).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "response" \ "results" \ "addresses").as[Array[Addresses]].length.equals(0) shouldBe true
      }

      "return with a status of " + BAD_REQUEST + " when payload with missing required fields" in {
        val result = route(application, FakeRequest(PUT, versionCtx + route_updateDeleteAddress).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc")).withJsonBody(invalidCreateAddressPayloadMissingFields)).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].length.equals(2) shouldBe true
      }

      "return with a status of " + OK + " when valid cookies with missing optional fields" in {
        val successfulAddressResponse = Future.successful((AddressesResponse(Seq.empty), Seq.empty, Seq.empty, 200))
        when(addressService.updateAddress(any[Map[String, String]], any[String], any[AddressRequest])).thenReturn(successfulAddressResponse)

        val result = route(application, FakeRequest(PUT, versionCtx + route_updateDeleteAddress).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc")).withJsonBody(validCreateAddressPayloadMissingOpt)).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "response" \ "results" \ "addresses").as[Seq[Addresses]].length.equals(0) shouldBe true
      }
      "return with a status of " + OK + " when valid cookies are present and is canadian postal code" in {
        val successfulAddressResponse = Future.successful((AddressesResponse(Seq.empty), Seq.empty, Seq.empty, 200))
        when(addressService.updateAddress(any[Map[String, String]], any[String], any[AddressRequest])).thenReturn(successfulAddressResponse)

        val result = route(application, FakeRequest(PUT, versionCtx + route_updateDeleteAddress).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc")).withJsonBody(validCreateAddressWithCanadianPostalCodePayload)).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "response" \ "results" \ "addresses").as[Array[Addresses]].length.equals(0) shouldBe true
      }

      "return with a status of " + BAD_REQUEST + " when valid cookies are present and canadian postal code is invalid" in {
        val result = route(application, FakeRequest(PUT, versionCtx + route_updateDeleteAddress).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc")).withJsonBody(createAddressWithInvalidCanadianPostalCodePayload)).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].length shouldBe 1
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]] foreach { error =>
          error.error should be("zip")
          error.data should be("Province and postal code do not match.")
        }
      }

      "return with a status of " + BAD_REQUEST + " when bad address type entered" in {
        val badAddress = new AddressRequestBuilder().withAddressType("GARBAGE").build()

        val result = route(application, FakeRequest(PUT, versionCtx + route_updateDeleteAddress).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc")).withJsonBody(badAddress)).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].length shouldBe 1
      }
    }

    "delete addresses action" should {
      "return with a status of " + UNAUTHORIZED + "  due to the absence of a \"JSESSIONID\" Cookie" in {
        val result = route(application, FakeRequest(DELETE, versionCtx + route_updateDeleteAddress)).get
        status(result) shouldBe UNAUTHORIZED
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + OK + " when valid cookies are present" in {
        val successfulAddressResponse = Future.successful((AddressesResponse(Seq.empty), Seq.empty, Seq.empty, 200))
        when(addressService.deleteAddress(any[Map[String, String]], any(), any[String])).thenReturn(successfulAddressResponse)

        val result = route(application, FakeRequest(DELETE, versionCtx + route_updateDeleteAddress).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "response" \ "results" \ "addresses").as[Array[Addresses]].length.equals(0) shouldBe true
      }

      "return with a status of " + BAD_REQUEST + " when bad address type queried" in {
        val result = route(application, FakeRequest(DELETE, versionCtx + "/account-service/accounts/123/addresses/1000?address_type=GARBAGE").withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].length shouldBe 1
      }
    }
  }

}
