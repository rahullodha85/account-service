package unit.controllers

import fixtures.{ RequestFixtures, RoutesPath }
import models.ApiErrorModel
import models.servicemodel.PaymentMethodModel
import models.website._
import org.mockito.Matchers.any
import org.mockito.Mockito.{ mock => _, _ }
import org.scalatest.{ BeforeAndAfterEach, Matchers, WordSpec }
import play.api.inject._
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.PaymentMethodService
import utils.TestUtils._

import scala.concurrent.Future

class PaymentMethodControllerSpec extends WordSpec
    with Matchers
    with BeforeAndAfterEach
    with RoutesPath
    with RequestFixtures {

  val paymentService = mock[PaymentMethodService]
  val application = createIsolatedApplication()
    .overrides(bind[PaymentMethodService].toInstance(paymentService))
    .build()

  override def beforeEach(): Unit = {
    reset(paymentService)
  }

  "PaymentMethod Controller" should {

    "Get Payment Method" should {

      "return with a status of " + UNAUTHORIZED + "  due to the absence of a \"JSESSIONID\" Cookie" in {
        val result = route(application, FakeRequest(GET, versionCtx + route_getPaymentMethod).withCookies(Cookie("UserName", "abc"))).get
        status(result) shouldBe UNAUTHORIZED
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + OK + " when expected \"UserName\" and \"JSESSIONID\" Cookies are present " in {
        val success = Future.successful(PaymentTabResponseWebsiteModel(true, Seq.empty, AddAnotherWebsiteModel(None), Seq.empty, None, None, Month(Seq.empty)), Seq.empty, Seq.empty, 200)
        when(paymentService.getPaymentMethod(any[Map[String, String]], any[String])).thenReturn(success)

        val result = route(application, FakeRequest(GET, versionCtx + route_getPaymentMethod).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "response" \ "results" \ "payment_methods_info").as[Array[PaymentMethodModel]].length.equals(0) shouldBe true
        header("cache-control", result).get shouldBe "max-age=0, no-cache"
      }
    }

    "Update Payment Method" should {

      "return with a status of " + UNAUTHORIZED + "  due to the absence of a \"JSESSIONID\" Cookie" in {
        val result = route(application, FakeRequest(PUT, versionCtx + route_updatePaymentMethod).withCookies(Cookie("UserName", "abc"))).get
        status(result) shouldBe UNAUTHORIZED
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + BAD_REQUEST + " when expected \"UserName\" and \"JSESSIONID\" Cookies are present but payload is not present " in {
        val result = route(application, FakeRequest(PUT, versionCtx + route_updatePaymentMethod).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + BAD_REQUEST + " when expected \"UserName\" and \"JSESSIONID\" Cookies are present but invalid payload is present " in {
        val result = route(application, FakeRequest(PUT, versionCtx + route_updatePaymentMethod).withJsonBody(inValidSinglePaymentMethodRequestPayload).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + OK + " when expected \"UserName\" and \"JSESSIONID\" Cookies are present " in {
        val success = Future.successful(PaymentTabPostResponseWebsiteModel(Seq.empty), Seq.empty, Seq.empty, 200)
        when(paymentService.updatePaymentMethod(any[Map[String, String]], any[String], any[UpdatePaymentMethodRequest])).thenReturn(success)

        val result = route(application, FakeRequest(PUT, versionCtx + route_updatePaymentMethod).withJsonBody(nonExpiredCreditCardSinglePaymentMethodRequestPayload).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
      }

      "return an empty payment method array" in {
        val success = Future.successful(PaymentTabPostResponseWebsiteModel(Seq.empty), Seq.empty, Seq.empty, 200)
        when(paymentService.updatePaymentMethod(any[Map[String, String]], any[String], any[UpdatePaymentMethodRequest])).thenReturn(success)

        val result = route(application, FakeRequest(PUT, versionCtx + route_updatePaymentMethod).withJsonBody(nonExpiredCreditCardSinglePaymentMethodRequestPayload).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe OK
        (contentAsJson(result) \ "response" \ "results" \ "payment_methods_info").as[Array[PaymentMethodModel]].length.equals(0) shouldBe true
      }

    }

    "Delete Payment Method" should {

      "return with a status of " + UNAUTHORIZED + "  due to the absence of a \"JSESSIONID\" Cookie" in {
        val result = route(application, FakeRequest(DELETE, versionCtx + route_deletePaymentMethod).withCookies(Cookie("UserName", "abc"))).get
        status(result) shouldBe UNAUTHORIZED
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + OK + " when expected \"UserName\" and \"JSESSIONID\" Cookies are present " in {
        val success = Future.successful(PaymentTabPostResponseWebsiteModel(Seq.empty), Seq.empty, Seq.empty, 200)
        when(paymentService.deletePaymentMethod(any[Map[String, String]], any[String])).thenReturn(success)

        val result = route(application, FakeRequest(DELETE, versionCtx + route_deletePaymentMethod).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
      }

      "return an empty payment method array" in {
        val success = Future.successful(PaymentTabPostResponseWebsiteModel(Seq.empty), Seq.empty, Seq.empty, 200)
        when(paymentService.deletePaymentMethod(any[Map[String, String]], any[String])).thenReturn(success)

        val result = route(application, FakeRequest(DELETE, versionCtx + route_deletePaymentMethod).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe OK
        (contentAsJson(result) \ "response" \ "results" \ "payment_methods_info").as[Array[PaymentMethodModel]].length.equals(0) shouldBe true
      }
    }

    "Create Payment Method" should {

      "return with a status of " + UNAUTHORIZED + "  due to the absence of a \"JSESSIONID\" Cookie" in {
        val result = route(application, FakeRequest(POST, versionCtx + route_createPaymentMethod).withCookies(Cookie("UserName", "abc"))).get
        status(result) shouldBe UNAUTHORIZED
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + OK + " when expected \"UserName\" and \"JSESSIONID\" Cookies are present " in {
        val success = Future.successful(PaymentTabPostResponseWebsiteModel(Seq.empty), Seq.empty, Seq.empty, 200)
        when(paymentService.createPaymentMethod(any[Map[String, String]], any[CreatePaymentMethodRequest], any())).thenReturn(success)

        val result = route(application, FakeRequest(POST, versionCtx + route_createPaymentMethod).withJsonBody(nonExpiredCreditCardSinglePaymentMethodRequestPayload).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + OK + "when brand is Saks Store card and month and year are null" in {
        val success = Future.successful(PaymentTabPostResponseWebsiteModel(Seq.empty), Seq.empty, Seq.empty, 200)
        when(paymentService.createPaymentMethod(any[Map[String, String]], any[CreatePaymentMethodRequest], any())).thenReturn(success)

        val result = route(application, FakeRequest(POST, versionCtx + route_createPaymentMethod).withJsonBody(buildCardPayload("SAKS", null, null)).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
      }

      "return status of " + BAD_REQUEST + " when card with expected expiration date(non-Saks card) has month as null" in {
        val result = route(application, FakeRequest(POST, versionCtx + route_createPaymentMethod).withJsonBody(buildCardPayload("DISC", 3000, null)).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe BAD_REQUEST
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].length shouldBe 1
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]] foreach { error =>
          error.error should be("month.invalid_expiration_date")
          error.data should be("Please enter a valid expiration date.")
        }
      }

      "return status of " + BAD_REQUEST + " when card with expected expiration date (non-Saks card) has year as null" in {
        val result = route(application, FakeRequest(POST, versionCtx + route_createPaymentMethod).withJsonBody(buildCardPayload("DISC", null, 10)).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe BAD_REQUEST
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].length shouldBe 2
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]] foreach { error =>
          error.error match {
            case "month.invalid_expiration_date" =>
              error.data should be("Please enter a valid expiration date.")
            case "year.invalid_expiration_year" =>
              error.data should be("Please enter a valid expiration year.")
          }
        }
      }

      "return status of " + BAD_REQUEST + " when card with expected expiration date(non-Saks card) has an expired date" in {
        val result = route(application, FakeRequest(POST, versionCtx + route_createPaymentMethod).withJsonBody(buildCardPayload("DISC", 2000, 10)).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe BAD_REQUEST
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].length shouldBe 2
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]] foreach { error =>
          error.error match {
            case "month.invalid_expiration_date" =>
              error.data should be("Please enter a valid expiration date.")
            case "year.invalid_expiration_year" =>
              error.data should be("Please enter a valid expiration year.")
          }
        }
      }

      "return status of " + OK + " and no errors when a non-expired credit card is added." in {
        val success = Future.successful(PaymentTabPostResponseWebsiteModel(Seq.empty), Seq.empty, Seq.empty, 200)
        when(paymentService.createPaymentMethod(any[Map[String, String]], any[CreatePaymentMethodRequest], any())).thenReturn(success)

        val result = route(application, FakeRequest(POST, versionCtx + route_createPaymentMethod).withJsonBody(nonExpiredCreditCardSinglePaymentMethodRequestPayload).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe OK
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].length shouldBe 0
      }
    }
  }
}
