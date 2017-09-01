package unit.service

import constants.Banners
import helpers.{ AccountHelper, ConfigHelper, TogglesHelper }
import models._
import models.servicemodel._
import models.website._
import org.mockito.Matchers.{ any, eq => eql }
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }
import play.api.mvc.Cookie
import services.{ HttpTransportService, _ }
import utils.NoOpStatsDClient
import utils.TestUtils._

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Right

class PaymentMethodServiceSpec
    extends WordSpec
    with MockitoSugar
    with Matchers {

  private val injector = createIsolatedApplication().build().injector
  val accountHelper = injector.instanceOf[AccountHelper]
  val headers: Map[String, String] = Map("JSESSIONID" -> "123")

  "PaymentMethod Service" should {
    val card: PaymentMethodModel = PaymentMethodModel("VISA", 1, false, 2, "George", "411111111111", "212", "user_id", false, false, 2016)
    val cards: Seq[PaymentMethodModel] = Seq(card, card.copy(brand = "SAKS", name = "Andrew"))

    "Get Payment Method" should {

      val togglesHelper = mock[TogglesHelper]
      when(togglesHelper.getFavoritesToggleState).thenReturn(Future.successful(false))
      when(togglesHelper.saksFirstPageEnabled).thenReturn(Future.successful(false))
      "return payment model with a single payment method, cookies, empty errors and 200 has status code for successful response" in {

        val httpTransportService = mock[HttpTransportService]
        val rewardsService = mock[RewardsService]
        when(httpTransportService.getFromService[PaymentMethodsModel](any(), any(), any(), any())(any())).thenReturn(Future.successful(Right(SuccessfulResponse(PaymentMethodsModel(cards), Seq.empty[Cookie]))))

        val paymentMethodService = new PaymentMethodService(httpTransportService, accountHelper, NoOpStatsDClient, togglesHelper, ConfigHelper, rewardsService)

        whenReady(paymentMethodService.getPaymentMethod(headers, "some/path")) {
          case (addressRes, cookies, errors, code) =>
            addressRes.enabled should be(true)
            addressRes.payment_methods_info.size should be(2)
            cookies.isEmpty should be(true)
            errors.isEmpty should be(true)
            code should be(200)
        }
      }

      "return payment model with enabled flag equal to false for multiple payment method with no returned payment methods,cookies, with non empty errors and 200 has status code for successful response" in {
        val httpTransportService = mock[HttpTransportService]
        val rewardsService = mock[RewardsService]

        val expectedErrorResponse = Future.successful(Left(FailureResponse(Seq(ApiErrorModel("Error here", "error.error")), 500)))
        when(httpTransportService.getFromService[PaymentMethodsModel](any(), any(), any(), any())(any())).thenReturn(expectedErrorResponse)
        val paymentMethodService = new PaymentMethodService(httpTransportService, accountHelper, NoOpStatsDClient, togglesHelper, ConfigHelper, rewardsService)

        whenReady(paymentMethodService.getPaymentMethod(headers, "some/path")) {
          case (addressRes, cookies, errors, code) =>
            addressRes.enabled should be(false)
            addressRes.payment_methods_info.size should be(0)
            cookies.isEmpty should be(true)
            errors.head.error should be("error.error")
            errors.head.data should be("Error here")
            code should be(200)
        }
      }
    }

    "Create Payment Method" should {
      "return payment model for all payment methods when a valid new credit card is submitted" in {
        val payments = new PaymentMethodsModel(Seq(new PaymentMethodModel("DISC", 123, true, 1, "", "", "", "test@test.com", true, true, 2300)))
        val httpTransportService = mock[HttpTransportService]
        val rewardsService = mock[RewardsService]

        when(httpTransportService.postToService[CreatePaymentMethodRequest, PaymentMethodsModel](any(), any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(SuccessfulResponse(payments))))

        val paymentMethodService = new PaymentMethodService(httpTransportService, accountHelper, NoOpStatsDClient, mock[TogglesHelper], ConfigHelper, rewardsService)

        val createPaymentAction = paymentMethodService.createPaymentMethod(headers, new CreatePaymentMethodRequest(true, "DISC", "", "", Some(1), Some(2), None), "")
        whenReady(createPaymentAction) {
          case (paymentMethod, cookies, errors, code) => {
            code shouldBe 200
            paymentMethod.payment_methods_info.head.display_brand_name should be("Discover")
            paymentMethod.payment_methods_info.head.allowed_operation should be(Map("edit_enabled" -> true, "delete_enabled" -> true))
            cookies.isEmpty shouldBe true
            errors.isEmpty shouldBe true
          }
        }
      }

      "still return a success even if linking your card fails" in {
        val payments = new PaymentMethodsModel(Seq(new PaymentMethodModel("DISC", 123, true, 1, "", "", "", "test@test.com", true, true, 2300)))
        val httpTransportService = mock[HttpTransportService]
        val rewardsService = mock[RewardsService]

        val rewardsWebsiteModel = RewardsWebsiteModel(enabled = true, "saksfirst", None, None, Some(SaksFirstModel(true, true, MemberInfo.empty)), None, None)

        when(httpTransportService.postToService[CreatePaymentMethodRequest, PaymentMethodsModel](any(), any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(SuccessfulResponse(payments))))
        when(rewardsService.linkSaksFirstAccount(any(), any(), any())).thenReturn(Future.successful(rewardsWebsiteModel, Seq.empty, Seq.empty, 500))

        val paymentMethodService = new PaymentMethodService(httpTransportService, accountHelper, NoOpStatsDClient, mock[TogglesHelper], ConfigHelper, rewardsService)

        val createPaymentAction = paymentMethodService.createPaymentMethod(headers, new CreatePaymentMethodRequest(true, "DISC", "", "", Some(1), Some(2), None), "")
        whenReady(createPaymentAction) {
          case (paymentMethod, cookies, errors, code) => {
            code shouldBe 200
            paymentMethod.payment_methods_info.head.display_brand_name should be("Discover")
            paymentMethod.payment_methods_info.head.allowed_operation should be(Map("edit_enabled" -> true, "delete_enabled" -> true))
            cookies.isEmpty shouldBe true
            errors.isEmpty shouldBe true
          }
        }
      }

      "return payment model with only delete enabled when paypal is submitted" in {
        val payments = new PaymentMethodsModel(Seq(new PaymentMethodModel("PAYPAL", 123, true, 1, "", "", "", "test@test.com", true, true, 2300)))
        val httpTransportService = mock[HttpTransportService]
        val rewardsService = mock[RewardsService]

        when(httpTransportService.postToService[CreatePaymentMethodRequest, PaymentMethodsModel](any(), any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(SuccessfulResponse(payments))))
        val paymentMethodService = new PaymentMethodService(httpTransportService, accountHelper, NoOpStatsDClient, mock[TogglesHelper], ConfigHelper, rewardsService)

        val request: CreatePaymentMethodRequest = CreatePaymentMethodRequest(true, "", "", "", Some(5), Some(2032), None)
        val createPaymentAction = paymentMethodService.createPaymentMethod(headers, request, "")
        whenReady(createPaymentAction) {
          case (paymentMethod, cookies, errors, code) => {
            code shouldBe 200
            paymentMethod.payment_methods_info.head.display_brand_name should be("PayPal")
            paymentMethod.payment_methods_info.head.credit_card.year should be(2300)
            paymentMethod.payment_methods_info.head.allowed_operation should be(Map("edit_enabled" -> false, "delete_enabled" -> true))
            paymentMethod.payment_methods_info.head.credit_card.user_id should be("test@test.com")
            cookies.isEmpty shouldBe true
            errors.isEmpty shouldBe true
          }
        }
      }
      "call rewards service to link if you have no existing saks card if banner is Saks" in {
        val payments = new PaymentMethodsModel(Seq(new PaymentMethodModel("DISC", 123, true, 1, "", "31311414141", "", "test@test.com", true, true, 2300)))
        val httpTransportService = mock[HttpTransportService]
        val togglesHelper = mock[TogglesHelper]

        val rewardsService = mock[RewardsService]
        val configHelper = mock[ConfigHelper]
        when(configHelper.banner).thenReturn(Banners.Saks)
        when(togglesHelper.getFavoritesToggleState).thenReturn(Future.successful(false))
        when(httpTransportService.postToService[CreatePaymentMethodRequest, PaymentMethodsModel](any(), any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(SuccessfulResponse(payments))))

        val paymentMethodService = new PaymentMethodService(httpTransportService, accountHelper, NoOpStatsDClient, togglesHelper, configHelper, rewardsService)

        val createPaymentAction = paymentMethodService.createPaymentMethod(headers, new CreatePaymentMethodRequest(true, "SAKS", "Don Biggs", "43234212", Some(1), Some(2), Some("12345")), "accountId")
        whenReady(createPaymentAction) {
          case (paymentMethod, cookies, errors, code) => {
            code shouldBe 200
            paymentMethod.payment_methods_info.head.allowed_operation should be(Map("edit_enabled" -> true, "delete_enabled" -> true))
            verify(rewardsService).linkSaksFirstAccount(any(), eql(LinkSaksFirstRequest("Don Biggs", "12345", "43234212")), any())
            cookies.isEmpty shouldBe true
            errors.isEmpty shouldBe true
          }
        }
      }

      "not call rewards service to link if you have an existing saks card" in {
        val payments = new PaymentMethodsModel(Seq(
          new PaymentMethodModel("SAKS", 123, true, 1, "", "", "", "test@test.com", true, true, 2300),
          new PaymentMethodModel("SAKS", 124, true, 1, "", "", "", "test@test.com", true, true, 2300)
        ))
        val httpTransportService = mock[HttpTransportService]
        val rewardsService = mock[RewardsService]
        val togglesHelper = mock[TogglesHelper]

        when(togglesHelper.getFavoritesToggleState).thenReturn(Future.successful(false))
        when(httpTransportService.postToService[CreatePaymentMethodRequest, PaymentMethodsModel](any(), any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(SuccessfulResponse(payments))))

        val paymentMethodService = new PaymentMethodService(httpTransportService, accountHelper, NoOpStatsDClient, mock[TogglesHelper], ConfigHelper, rewardsService)

        val createPaymentAction = paymentMethodService.createPaymentMethod(headers, new CreatePaymentMethodRequest(true, "SAKS", "", "", Some(1), Some(2), Some("12345")), "")
        whenReady(createPaymentAction) {
          case (paymentMethod, cookies, errors, code) => {
            code shouldBe 200
            paymentMethod.payment_methods_info.head.allowed_operation should be(Map("edit_enabled" -> true, "delete_enabled" -> true))
            verify(rewardsService, never()).linkSaksFirstAccount(any(), any(), any())
            cookies.isEmpty shouldBe true
            errors.isEmpty shouldBe true
          }
        }
      }

      "not call rewards service to link if you are a non-saks banner" in {
        val payments = new PaymentMethodsModel(Seq(new PaymentMethodModel("SAKS", 123, true, 1, "", "", "", "test@test.com", true, true, 2300)))
        val httpTransportService = mock[HttpTransportService]
        val rewardsService = mock[RewardsService]
        val togglesHelper = mock[TogglesHelper]
        val configHelper = mock[ConfigHelper]
        when(configHelper.banner).thenReturn(Banners.Off5th)
        when(togglesHelper.getFavoritesToggleState).thenReturn(Future.successful(false))
        when(httpTransportService.postToService[CreatePaymentMethodRequest, PaymentMethodsModel](any(), any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(SuccessfulResponse(payments))))

        val paymentMethodService = new PaymentMethodService(httpTransportService, accountHelper, NoOpStatsDClient, mock[TogglesHelper], configHelper, rewardsService)

        val createPaymentAction = paymentMethodService.createPaymentMethod(headers, new CreatePaymentMethodRequest(true, "SAKS", "", "", Some(1), Some(2), Some("12345")), "")
        whenReady(createPaymentAction) {
          case (paymentMethod, cookies, errors, code) => {
            code shouldBe 200
            paymentMethod.payment_methods_info.head.allowed_operation should be(Map("edit_enabled" -> true, "delete_enabled" -> true))
            verify(rewardsService, never()).linkSaksFirstAccount(any(), any(), any())
            cookies.isEmpty shouldBe true
            errors.isEmpty shouldBe true
          }
        }
      }

      "return error with status code when Transport service returns an error" in {
        val expectedErrorResponse = Seq(ApiErrorModel("Error here", "error.error"))
        val httpTransportService = mock[HttpTransportService]
        val rewardsService = mock[RewardsService]

        when(httpTransportService.postToService[CreatePaymentMethodRequest, PaymentMethodsModel](any(), any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(Left(FailureResponse(expectedErrorResponse, 400))))

        val paymentMethodService = new PaymentMethodService(httpTransportService, accountHelper, NoOpStatsDClient, mock[TogglesHelper], ConfigHelper, rewardsService)

        val request: CreatePaymentMethodRequest = CreatePaymentMethodRequest(true, "", "", "", Some(5), Some(2032), None)
        val createPaymentAction = paymentMethodService.createPaymentMethod(headers, request, "")
        whenReady(createPaymentAction) {
          case (paymentMethod, cookies, errors, code) => {
            code shouldBe 400
            errors shouldBe expectedErrorResponse
            cookies.isEmpty shouldBe true
          }
        }
      }
    }

    "Delete Payment Method" should {
      "return payment model for all payment methods when a valid new credit card is deleted" in {
        val payments = new PaymentMethodsModel(Seq(new PaymentMethodModel("DISC", 123, true, 1, "", "", "", "", true, true, 2300)))
        val httpTransportService = mock[HttpTransportService]
        val rewardsService = mock[RewardsService]

        when(httpTransportService.deleteFromService[PaymentMethodsModel](any(), any(), any(), any())(any())).thenReturn(Future.successful(Right(SuccessfulResponse(payments))))
        val paymentMethodService = new PaymentMethodService(httpTransportService, accountHelper, NoOpStatsDClient, mock[TogglesHelper], ConfigHelper, rewardsService)

        val deletePayment = paymentMethodService.deletePaymentMethod(headers, "1234")
        whenReady(deletePayment) {
          case (paymentMethod, cookies, errors, code) => {
            code shouldBe 200
            paymentMethod.payment_methods_info.head.display_brand_name should be("Discover")
            paymentMethod.payment_methods_info.head.allowed_operation should be(Map("edit_enabled" -> true, "delete_enabled" -> true))
            cookies.isEmpty shouldBe true
            errors.isEmpty shouldBe true
          }
        }
      }

      "return error with status code when Transport service returns an error" in {
        val expectedErrorResponse = FailureResponse(Seq(ApiErrorModel("Error here", "error.error")), 400)
        val httpTransportService = mock[HttpTransportService]
        val rewardsService = mock[RewardsService]

        when(httpTransportService.deleteFromService[PaymentMethodsModel](any(), any(), any(), any())(any())).thenReturn(Future.successful(Left(expectedErrorResponse)))
        val paymentMethodService = new PaymentMethodService(httpTransportService, accountHelper, NoOpStatsDClient, mock[TogglesHelper], ConfigHelper, rewardsService)

        val createPaymentAction = paymentMethodService.deletePaymentMethod(headers, "1234")
        whenReady(createPaymentAction) {
          case (paymentMethod, cookies, errors, code) => {
            code shouldBe 400
            errors shouldBe expectedErrorResponse.errors
            cookies.isEmpty shouldBe true
          }
        }
      }
    }

    "Edt Payment Method" should {

      "return payment model for all payment methods when a credit card is edited" in {
        val httpTransportService = mock[HttpTransportService]
        val rewardsService = mock[RewardsService]

        when(httpTransportService.putToService[UpdatePaymentMethodRequest, PaymentMethodsModel](any(), any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(SuccessfulResponse(PaymentMethodsModel(cards), Seq.empty[Cookie]))))

        val paymentMethodService = new PaymentMethodService(httpTransportService, accountHelper, NoOpStatsDClient, mock[TogglesHelper], ConfigHelper, rewardsService)
        val createPaymentAction = paymentMethodService.updatePaymentMethod(headers, "someId", new UpdatePaymentMethodRequest(1L, true, "", "", Some(1), Some(2020)))
        whenReady(createPaymentAction) {
          case (paymentMethod, cookies, errors, code) => {
            code shouldBe 200
            paymentMethod.payment_methods_info.head.display_brand_name should be("Visa")
            paymentMethod.payment_methods_info.head.allowed_operation should be(Map("edit_enabled" -> true, "delete_enabled" -> true))
            cookies.isEmpty shouldBe true
            errors.isEmpty shouldBe true
          }
        }
      }

      "return error with status code when Transport service returns an error" in {
        val httpTransportService = mock[HttpTransportService]
        val rewardsService = mock[RewardsService]

        val expectedErrorResponse = FailureResponse(Seq(ApiErrorModel("Error here", "error.error")), 400)
        when(httpTransportService.putToService[UpdatePaymentMethodRequest, PaymentMethodsModel](any(), any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(Left(expectedErrorResponse)))

        val paymentMethodService = new PaymentMethodService(httpTransportService, accountHelper, NoOpStatsDClient, mock[TogglesHelper], ConfigHelper, rewardsService)

        val createPaymentAction = paymentMethodService.updatePaymentMethod(headers, "someId", new UpdatePaymentMethodRequest(1L, true, "", "", Some(1), Some(2020)))
        whenReady(createPaymentAction) {
          case (paymentMethod, cookies, errors, code) => {
            code shouldBe 400
            errors shouldBe expectedErrorResponse.errors
            cookies.isEmpty shouldBe true
          }
        }
      }
    }
  }
}
