package unit.service

import java.util.concurrent.TimeoutException

import constants.Constants._
import fixtures.RequestFixtures
import helpers.{ AccountHelper, ConfigHelper, TogglesHelper }
import models.servicemodel._
import models.website._
import models.{ ApiErrorModel, FailureResponse, SuccessfulResponse }
import org.joda.time.DateTime
import org.mockito.Matchers.{ any, eq => eqTo }
import org.mockito.Mockito.{ verify, when }
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }
import play.api.Application
import play.api.mvc.Cookie
import services.{ HttpTransportService, _ }
import utils.NoOpStatsDClient
import utils.TestUtils._
import validations.Validator

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Right

class AccountServiceSpec extends WordSpec with MockitoSugar
    with Matchers
    with RequestFixtures {

  private val application: Application = createIsolatedApplication().build()
  val accountHelper = application.injector.instanceOf[AccountHelper]
  val validator = application.injector.instanceOf[Validator]

  "Account Service" should {
    "Summary" should {
      "return only the first 5 orders for a user if there are more than 5" in {
        val orders = Seq(mock[Order], mock[Order], mock[Order], mock[Order], mock[Order], mock[Order])
        val orderHistory = Orders(orders, PageInfo(1, 10, 6, 6))
        val fakeTogglesHelper: TogglesHelper = mock[TogglesHelper]
        val httpTransportService = mock[HttpTransportService]
        val rewardsService: RewardsService = mock[RewardsService]

        val successfulAccountResponse = Future.successful(Right(SuccessfulResponse(UserAccount(Some("12345"), "mail@mail.com", "george", Some("92928383838"), "mail@mail.com", Some(""), "someName", Some("a uuid")))))
        val successfulOrderHistoryResponse = Future.successful(Right(SuccessfulResponse[Orders](orderHistory)))

        when(fakeTogglesHelper.getFavoritesToggleState).thenReturn(Future.successful(false))
        when(fakeTogglesHelper.saksFirstPageEnabled).thenReturn(Future.successful(false))
        when(rewardsService.retrieveMoreAccount(UserAccount(Some("12345"), "mail@mail.com", "george", Some("92928383838"), "mail@mail.com", Some(""), "someName", Some("a uuid")), Map("JSESSIONID" -> "123"))).thenReturn(Future.successful(Right(SuccessfulResponse(More("1234")))))
        when(httpTransportService.getFromService[Orders](ConfigHelper.getStringProp("data-service.order"), Some("orders"), Map("JSESSIONID" -> "123"), Map(PAGE_NUM_PARAM -> "1"))).thenReturn(successfulOrderHistoryResponse)
        when(httpTransportService.getFromService[UserAccount](ConfigHelper.getStringProp("data-service.user-account"), Some("get_account"), Map("JSESSIONID" -> "123")))
          .thenReturn(successfulAccountResponse)

        val accountService = new AccountService(httpTransportService, rewardsService, localizationService, NoOpStatsDClient, fakeTogglesHelper, accountHelper, ConfigHelper)
        whenReady(accountService.getAccountSummary("12345")(Map("JSESSIONID" -> "123"))) {
          case (summaryResponse, cookies, errors, code) =>
            summaryResponse.order_history.orders.size shouldBe 5
            summaryResponse.order_history.page_info.current_items shouldBe 5
            summaryResponse.order_history.page_info.page_size shouldBe 5
        }

      }

      "return all orders for a user if there are less than 5" in {
        val orders = Seq(mock[Order], mock[Order], mock[Order])
        val orderHistory = Orders(orders, PageInfo(1, 10, 3, 3))

        val fakeTogglesHelper: TogglesHelper = mock[TogglesHelper]
        val httpTransportService = mock[HttpTransportService]
        val rewardsService: RewardsService = mock[RewardsService]
        val successfulAccountResponse = Future.successful(Right(SuccessfulResponse(UserAccount(Some("12345"), "mail@mail.com", "george", Some("92928383838"), "mail@mail.com", Some(""), "someName", Some("a uuid")))))

        when(fakeTogglesHelper.getFavoritesToggleState).thenReturn(Future.successful(false))
        when(fakeTogglesHelper.saksFirstPageEnabled).thenReturn(Future.successful(false))

        when(httpTransportService.getFromService[Orders](ConfigHelper.getStringProp("data-service.order"), Some("orders"), Map("JSESSIONID" -> "123"), Map(PAGE_NUM_PARAM -> "1"))).thenReturn(Future.successful(Right(SuccessfulResponse(orderHistory, Seq.empty[Cookie]))))
        when(rewardsService.retrieveMoreAccount(SuccessfulResponse(UserAccount(Some("12345"), "mail@mail.com", "george", Some("92928383838"), "mail@mail.com", Some(""), "someName", Some("a uuid")), Seq.empty[Cookie]).body, Map("JSESSIONID" -> "123")))
          .thenReturn(Future.successful(Right(SuccessfulResponse(More("1234")))))
        when(httpTransportService.getFromService[UserAccount](ConfigHelper.getStringProp("data-service.user-account"), Some("get_account"), Map("JSESSIONID" -> "123")))
          .thenReturn(successfulAccountResponse)

        val accountService = new AccountService(httpTransportService, rewardsService, localizationService, NoOpStatsDClient, fakeTogglesHelper, accountHelper, ConfigHelper)

        whenReady(accountService.getAccountSummary("12345")(Map("JSESSIONID" -> "123"))) {
          case (summaryResponse, cookies, errors, code) =>
            summaryResponse.order_history.orders.size shouldBe 3
            summaryResponse.order_history.page_info.current_items shouldBe 3
            summaryResponse.order_history.page_info.page_size shouldBe 5
        }
      }

      "return user an empty more response when more service times out" in {
        val fakeTogglesHelper: TogglesHelper = mock[TogglesHelper]
        val httpTransportService = mock[HttpTransportService]
        val rewardsService = mock[RewardsService]
        val successfulAccountResponse = Future.successful(Right(SuccessfulResponse(UserAccount(Some("12345"), "mail@mail.com", "george", Some("92928383838"), "mail@mail.com", Some(""), "someName", Some("a uuid")))))

        when(fakeTogglesHelper.getFavoritesToggleState).thenReturn(Future.successful(false))
        when(fakeTogglesHelper.saksFirstPageEnabled).thenReturn(Future.successful(false))

        when(rewardsService.retrieveMoreAccount(any(), any())).thenReturn(Future.failed(new TimeoutException("Client took too long to respond.")))
        when(httpTransportService.getFromService[Orders](ConfigHelper.getStringProp("data-service.order"), Some("orders"), Map("JSESSIONID" -> "123"), Map(PAGE_NUM_PARAM -> "1"))).thenReturn(Future.successful(Right(SuccessfulResponse(Orders(Seq.empty[Order], PageInfo(0, 0, 0, 0)), Seq.empty[Cookie]))))
        when(httpTransportService.getFromService[UserAccount](ConfigHelper.getStringProp("data-service.user-account"), Some("get_account"), Map("JSESSIONID" -> "123")))
          .thenReturn(successfulAccountResponse)

        val accountService = new AccountService(httpTransportService, rewardsService, localizationService, NoOpStatsDClient, fakeTogglesHelper, accountHelper, ConfigHelper)

        whenReady(accountService.getAccountSummary("12345")(Map("JSESSIONID" -> "123"))) { response =>
          verify(rewardsService).retrieveMoreAccount(SuccessfulResponse(UserAccount(Some("12345"), "mail@mail.com", "george", Some("92928383838"), "mail@mail.com", Some(""), "someName", Some("a uuid")), Seq.empty[Cookie]).body, Map("JSESSIONID" -> "123"))
          response._1.account_profile.enabled should be(true)
        }
      }
    }

    "Sign out" should {
      "return valid response with redirect url over HTTP" in {
        val httpTransportService = mock[HttpTransportService]
        when(httpTransportService.getFromService[SuccessResponse](any(), any(), any(), any())(any())).thenReturn(Future.successful(Right(SuccessfulResponse(new SuccessResponse(true), Seq.empty[Cookie]))))

        val accountService: AccountService = new AccountService(httpTransportService, mock[RewardsService], localizationService, NoOpStatsDClient, mock[TogglesHelper], accountHelper, ConfigHelper)

        whenReady(accountService.logout(Map("JSESSIONID" -> "123"))) { response =>
          response._1.get.links.home_page_link should be("/")
          response._4 should be(200)
          response._2.isEmpty should be(true)
          response._3.isEmpty should be(true)
        }
      }

      "return an api error response with http transport returns an error" in {
        val httpTransportService = mock[HttpTransportService]
        when(httpTransportService.getFromService[SuccessResponse](any(), any(), any(), any())(any())).thenReturn(Future.successful(Left(FailureResponse(Seq(new ApiErrorModel("data", "error")), 400))))
        val accountService: AccountService = new AccountService(httpTransportService, mock[RewardsService], localizationService, NoOpStatsDClient, mock[TogglesHelper], accountHelper, ConfigHelper)

        whenReady(accountService.logout(Map("JSESSIONID" -> "123"))) { response =>
          response._1 should be(None)
          response._2.isEmpty should be(true)
          response._3.head.data should be("data")
          response._3.head.error should be("error")
          response._4 should be(400)
        }
      }
    }

    "Sign in Account" should {
      "return with valid sign in model,cookies empty errors and 200 has status code for successfulAccountResponse Response" in {
        val httpTransportService = mock[HttpTransportService]
        when(httpTransportService.postToService[ClientSignInRequest, UserSignInAuthentication](any(), any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(SuccessfulResponse(new UserSignInAuthentication(Some("123"), "", "", None, "", None)))))
        val accountService: AccountService = new AccountService(httpTransportService, mock[RewardsService], localizationService, NoOpStatsDClient, mock[TogglesHelper], accountHelper, ConfigHelper)

        val validLogin = ClientSignInRequest("abc235@abc235.com", "saks123456")
        val signInAction = accountService.signInAction(Map("JSESSIONID" -> "123"), validLogin)
        whenReady(signInAction) {
          case (signInActionJson, cookies, errors, code) =>
            code shouldBe 200
            cookies.isEmpty shouldBe true
            errors.isEmpty shouldBe true
        }
      }

      "return with expected model,cookies, errors and code from response for Failure Response" in {
        val httpTransportService = mock[HttpTransportService]
        when(httpTransportService.postToService[ClientSignInRequest, UserSignInAuthentication](any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Left(FailureResponse(Seq(new ApiErrorModel("data", "error")), 400))))

        val accountService: AccountService = new AccountService(httpTransportService, mock[RewardsService], localizationService, NoOpStatsDClient, mock[TogglesHelper], accountHelper, ConfigHelper)

        val invalidLogin = ClientSignInRequest("abc235?#abc235.com", "saks1234")
        val signInAction = accountService.asInstanceOf[AccountService].signInAction(Map("JSESSIONID" -> "123"), invalidLogin)
        whenReady(signInAction) {
          case (signInActionJson, cookies, errors, code) =>
            code shouldBe 400
            cookies.isEmpty shouldBe true
            errors.isEmpty shouldBe false
        }
      }

      "return with locked_user_account.error when receiving BM locked response" in {
        val httpTransportService = mock[HttpTransportService]
        when(httpTransportService.postToService[ClientSignInRequest, UserSignInAuthentication](any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Left(FailureResponse(Seq(new ApiErrorModel("data", "error")), 401))))

        val accountService: AccountService = new AccountService(httpTransportService, mock[RewardsService], localizationService, NoOpStatsDClient, mock[TogglesHelper], accountHelper, ConfigHelper)

        val validLogin = ClientSignInRequest("abc235@abc235.com", "saks123456")
        val signInAction = accountService.signInAction(Map("JSESSIONID" -> "123"), validLogin)
        whenReady(signInAction) {
          case (signInActionJson, cookies, errors, code) =>
            code shouldBe 401
            cookies.isEmpty shouldBe true
        }
      }
    }

    "Forgot Password Account" should {
      val forgotPassWordRequest: ForgotPasswordRequest = ForgotPasswordRequest("incredibles@incredibles.com")

      "return with valid forgot password model,cookies empty errors and 200 has status code for successfulAccountResponse Response over HTTP" in {
        val httpTransportService = mock[HttpTransportService]
        when(httpTransportService.postToService[ForgotPasswordRequest, SuccessResponse](any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(SuccessfulResponse(SuccessResponse(true)))))

        val accountService = new AccountService(httpTransportService, mock[RewardsService], localizationService, NoOpStatsDClient, mock[TogglesHelper], accountHelper, ConfigHelper)

        whenReady(accountService.forgotPasswordAction(Map("JSESSIONID" -> "123"), forgotPassWordRequest)) {
          case (forgotPasswordActionJson, cookies, errors, code) =>
            forgotPasswordActionJson.success should be(true)
            cookies.isEmpty should be(true)
            errors.isEmpty should be(true)
            code shouldBe 200
        }
      }

      "return with expected model,cookies and errors and 500 has status code for Failure Response" in {
        val httpTransportService = mock[HttpTransportService]
        when(httpTransportService.postToService[ForgotPasswordRequest, SuccessResponse](any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Left(FailureResponse(Seq(new ApiErrorModel("test data", "test error code")), 400))))

        val accountService: AccountService = new AccountService(httpTransportService, mock[RewardsService], localizationService, NoOpStatsDClient, mock[TogglesHelper], accountHelper, ConfigHelper)

        whenReady(accountService.forgotPasswordAction(Map("JSESSIONID" -> "123"), forgotPassWordRequest)) {
          case (forgotPasswordActionJson, cookies, errors, code) =>
            forgotPasswordActionJson.success should be(false)
            cookies.isEmpty should be(true)
            errors.head.data should be("test data")
            errors.head.error should be("test error code")
            code shouldBe 400
        }
      }
    }

    "Change Password Account" should {
      "return success is true, cookies empty errors and 200 has status code for successfulAccountResponse Response" in {
        val httpTransportService = mock[HttpTransportService]
        when(httpTransportService.postToService[ChangePasswordRequest, UserAuthChangePassword](any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(SuccessfulResponse(new UserAuthChangePassword("", None, "", None, "")))))

        val accountService: AccountService = new AccountService(httpTransportService, mock[RewardsService], localizationService, NoOpStatsDClient, mock[TogglesHelper], accountHelper, ConfigHelper)

        val changePasswordAction = accountService.changePasswordAction(Map("JSESSIONID" -> "123"), validChangePasswordPayload.as[ChangePasswordRequest])
        whenReady(changePasswordAction) {
          case (changePasswordActionResponse, cookies, errors, code) =>
            changePasswordActionResponse.success should be(true)
            cookies.isEmpty should be(true)
            errors.isEmpty should be(true)
            code shouldBe 200
        }
      }

      "return with expected model,cookies and errors and status code 500 for error response" in {
        val httpTransportService = mock[HttpTransportService]
        when(httpTransportService.postToService[ChangePasswordRequest, UserAuthChangePassword](any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Left(FailureResponse(Seq(new ApiErrorModel("test data", "test error code")), 500))))

        val accountService: AccountService = new AccountService(httpTransportService, mock[RewardsService], localizationService, NoOpStatsDClient, mock[TogglesHelper], accountHelper, ConfigHelper)

        val changePasswordAction = accountService.changePasswordAction(Map("JSESSIONID" -> "123"), validChangePasswordPayload.as[ChangePasswordRequest])
        whenReady(changePasswordAction) {
          case (changePasswordActionResponse, cookies, errors, code) =>
            changePasswordActionResponse.success should be(false)
            cookies.isEmpty should be(true)
            errors.isEmpty should be(false)
            code shouldBe 500
        }
      }
    }

    "Email Preference/Account Settings" should {
      "retrieve user account preferences and email prefs over http" in {
        val httpTransportService = mock[HttpTransportService]

        val eventualResponse: Future[Either[FailureResponse, SuccessfulResponse[Seq[EmailPreferencesModel]]]] = Future.successful(Right(SuccessfulResponse(Seq(EmailPreferencesModel(Some("T"), Some("T"), Some("T"), Some("T"))))))
        when(httpTransportService.getFromService[UserAccount](any(), eqTo(Some("get_account")), any(), any())(any())).thenReturn(Future.successful(Right(SuccessfulResponse(UserAccount(Some("12345"), "mail@mail.com", "george", Some("92928383838"), "mail@mail.com", Some(""), "someName", Some("a uuid")), Seq.empty[Cookie]))))
        when(httpTransportService.getFromService[Seq[EmailPreferencesModel]](any(), eqTo(Some(s"profile/${UserAccount(Some("12345"), "mail@mail.com", "george", Some("92928383838"), "mail@mail.com", Some(""), "someName", Some("a uuid")).user_id}")), any(), any())(any())).thenReturn(eventualResponse)

        val togglesHelper = mock[TogglesHelper]
        when(togglesHelper.getFavoritesToggleState).thenReturn(Future.successful(false))
        when(togglesHelper.saksFirstPageEnabled).thenReturn(Future.successful(false))

        val service: AccountService = new AccountService(httpTransportService, mock[RewardsService], localizationService, NoOpStatsDClient, togglesHelper, accountHelper, ConfigHelper)
        whenReady(service.getAccountSettings(Map("JSESSIONID" -> "123"), "12345")) { eventualResponse =>
          val expectedOptions = Seq(SaksOptStatus, Off5thOptStatus, SaksCanadaOptStatus, Off5thCanadaOptStatus)
          expectedOptions.forall(opt => eventualResponse._1.email_preferences.email_subscriptions.value.contains(opt)) should be(true)
          eventualResponse._2.size should be(0)
          eventualResponse._3.size should be(0)
          eventualResponse._4 should be(200)
        }
      }

      "update user account with preferences over http when http toggle is on" in {
        val preferencesRequestModel = PreferencesRequestModel("T", "F", "F", "F", "abc@abc.com", "123.45.67.890", DateTime.now().toString("yyyy-MM-dd'T'HH:mm:ssZ"))

        val eventualResponse: Future[Either[FailureResponse, SuccessfulResponse[Seq[EmailPreferencesModel]]]] = Future.successful(Right(SuccessfulResponse(Seq(EmailPreferencesModel(Some("F"), Some("T"), Some("F"), Some("F"))))))

        val httpTransportService: HttpTransportService = mock[HttpTransportService]
        when(httpTransportService.putToService[UpdateAccountRequest, Seq[EmailPreferencesModel]](any(), eqTo(Some(s"profile/abc@abc.com/preferences")), any(), any(), any())(any(), any())).thenReturn(eventualResponse)

        val togglesHelper = mock[TogglesHelper]

        val service: AccountService = new AccountService(httpTransportService, mock[RewardsService], localizationService, NoOpStatsDClient, togglesHelper, accountHelper, ConfigHelper)

        whenReady(service.updateEmailPreferences(preferencesRequestModel, Map("JSESSIONID" -> "123"))) { response =>
          response._1.preferences should be(Seq(SaksOptStatus))
        }
      }
    }

    "Reset Password Account" should {
      "return with valid sign in model,cookies empty errors and 200 has status code for successfulAccountResponse Response" in {
        val httpTransportService = mock[HttpTransportService]
        when(httpTransportService.postToService[ResetPasswordRequest, UserAuthResetPassword](any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(SuccessfulResponse(new UserAuthResetPassword("first", "last", "", Some(""), false)))))
        val accountService: AccountService = new AccountService(httpTransportService, mock[RewardsService], localizationService, NoOpStatsDClient, mock[TogglesHelper], accountHelper, ConfigHelper)

        val changePasswordAction = accountService.resetPasswordAction(Map("JSESSIONID" -> "123"), validResetPasswordPayload.as[ResetPasswordRequest], Loc = "loc12345")
        whenReady(changePasswordAction) {
          case (resetPasswordActionResponse, cookies, errors, code) =>
            resetPasswordActionResponse.is_account_cleared should be(false)
            resetPasswordActionResponse.first_name should be("first")
            resetPasswordActionResponse.last_name should be("last")
            cookies.isEmpty should be(true)
            errors.isEmpty should be(true)
            code shouldBe 200
        }
      }

      "return with expected model,cookies and errors and 400 has status code for Failure Response" in {
        val httpTransportService = mock[HttpTransportService]
        when(httpTransportService.postToService[ResetPasswordRequest, UserAuthResetPassword](any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Left(FailureResponse(Seq(new ApiErrorModel("data", "error")), 400))))

        val accountService: AccountService = new AccountService(httpTransportService, mock[RewardsService], localizationService, NoOpStatsDClient, mock[TogglesHelper], accountHelper, ConfigHelper)

        val changePasswordAction = accountService.resetPasswordAction(Map("JSESSIONID" -> "123"), validResetPasswordPayload.as[ResetPasswordRequest], Loc = "loc12345")
        whenReady(changePasswordAction) {
          case (resetPasswordActionResponse, cookies, errors, code) =>
            resetPasswordActionResponse.is_account_cleared should be(false)
            resetPasswordActionResponse.first_name should not be null
            resetPasswordActionResponse.last_name should not be null
            cookies.isEmpty should be(true)
            errors.isEmpty should be(false)
            code shouldBe 400
        }
      }
    }
  }
}
