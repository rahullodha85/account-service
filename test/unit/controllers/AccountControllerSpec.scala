package unit.controllers

import constants.Banners
import constants.Constants._
import models.ApiErrorModel
import models.servicemodel.{ SuccessResponse, UserAccount }
import models.website._
import org.mockito.Matchers.any
import org.mockito.Mockito.{ reset, when }
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterEach, Matchers, WordSpec }
import play.api.inject._
import play.api.libs.json.Json
import play.api.mvc.{ Cookie, Result }
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AccountService
import utils.TestUtils._

import scala.concurrent.Future
import scala.language.postfixOps

class AccountControllerSpec extends WordSpec
    with MockitoSugar
    with Matchers
    with BeforeAndAfterEach {

  val accountService = mock[AccountService]
  val application = createIsolatedApplication()
    .overrides(bind[AccountService].toInstance(accountService))
    .build()

  override def beforeEach(): Unit = {
    reset(accountService)
  }

  import utils.TestUtils._

  "Account Controller" should {

    "sign in action" should {

      "return with a status of " + OK + "regardless of any Cookie value" in {
        val signInResponse = Future.successful((None, Seq.empty, Seq.empty, 200))
        when(accountService.signInAction(any[Map[String, String]], any[ClientSignInRequest])(any())).thenReturn(signInResponse)

        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/sign-in").withJsonBody(Json.toJson(ClientSignInRequest("abc@email.test", "test123")))).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + BAD_REQUEST + " due to the absence of a request body" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/sign-in").withCookies(Cookie("JSESSIONID", "123"))).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
      }

      "return with 400 status code and messages saying that username and password are required if username and password are empty" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/sign-in").withJsonBody(Json.obj())).get
        status(result) shouldBe BAD_REQUEST
        (contentAsJson(result) \ "errors" \\ "data")(0).as[String] shouldBe "Username is required."
        (contentAsJson(result) \ "errors" \\ "error")(0).as[String] shouldBe "username"
        (contentAsJson(result) \ "errors" \\ "data")(1).as[String] shouldBe "Password is required."
        (contentAsJson(result) \ "errors" \\ "error")(1).as[String] shouldBe "password"
      }

      "return with a status of " + OK + " when given a valid request in payload and \"JSESSIONID\" cookie is present" in {
        val signInResponse = Future.successful((None, Seq.empty, Seq.empty, 200))
        when(accountService.signInAction(any[Map[String, String]], any[ClientSignInRequest])(any())).thenReturn(signInResponse)

        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/sign-in").withJsonBody(Json.toJson(ClientSignInRequest("abc@email.test", "test123")))).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "response" \ "results") should not be Nil
      }

      "return with a status of " + BAD_REQUEST + " when given an invalid request in payload and \"JSESSIONID\" are present" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/sign-in").withCookies(Cookie("JSESSIONID", "123")).withJsonBody(Json.toJson(ClientSignInRequest("abc235?#abc235.com", "")))).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true

        (contentAsJson(result) \ "errors" \\ "data")(0).as[String] shouldBe "Enter a valid email address."
        (contentAsJson(result) \ "errors" \\ "error")(0).as[String] shouldBe "username"
        (contentAsJson(result) \ "errors" \\ "data")(1).as[String] shouldBe "Password is required."
        (contentAsJson(result) \ "errors" \\ "error")(1).as[String] shouldBe "password"
      }
    }

    "csr sign in action" should {
      "return with a status of " + OK + " regardless of any Cookie value" in {
        val signInResponse = Future.successful((None, Seq.empty, Seq.empty, 200))
        when(accountService.signInAction(any[Map[String, String]], any[CSRSignInRequest])(any())).thenReturn(signInResponse)

        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/sign-in-csr-action").withJsonBody(Json.toJson(CSRSignInRequest("abc@email.test", "csr")))).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].isEmpty shouldBe true
      }

      "return with a status of " + BAD_REQUEST + " when given an invalid request in payload and \"JSESSIONID\" are present" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/sign-in-csr-action").withCookies(Cookie("JSESSIONID", "123")).withJsonBody(Json.toJson(CSRSignInRequest("abc#email.test", "")))).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true

        (contentAsJson(result) \ "errors" \\ "data")(0).as[String] shouldBe "Enter a valid email address."
        (contentAsJson(result) \ "errors" \\ "error")(0).as[String] shouldBe "username"
        (contentAsJson(result) \ "errors" \\ "data")(1).as[String] shouldBe "This field is required."
        (contentAsJson(result) \ "errors" \\ "error")(1).as[String] shouldBe "site_refer"
      }

      "return with 400 status code and messages saying that username and password are required if username and password are empty" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/sign-in-csr-action").withJsonBody(Json.obj())).get
        status(result) shouldBe BAD_REQUEST
        (contentAsJson(result) \ "errors" \\ "data")(0).as[String] shouldBe "Username is required."
        (contentAsJson(result) \ "errors" \\ "error")(0).as[String] shouldBe "username"
        (contentAsJson(result) \ "errors" \\ "data")(1).as[String] shouldBe "This field is required."
        (contentAsJson(result) \ "errors" \\ "error")(1).as[String] shouldBe "site_refer"
      }
    }

    "forgot password action" should {

      "return with a status of " + OK + "regardless of any Cookie value" in {
        val successResponse = Future.successful((SuccessResponse(true), Seq.empty, Seq.empty, 200))
        when(accountService.forgotPasswordAction(any[Map[String, String]], any[ForgotPasswordRequest])).thenReturn(successResponse)

        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/forgot-password").withJsonBody(Json.toJson(ForgotPasswordRequest("abec@email.test")))).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + BAD_REQUEST + " due to the absence of a request body" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/forgot-password").withCookies(Cookie("JSESSIONID", "123"))).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + BAD_REQUEST + " due to the absence of a request body and \"JSESSIONID\" cookie is present" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/forgot-password").withCookies(Cookie("JSESSIONID", "123")).withJsonBody(Json.obj())).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + OK + " when given a valid request in payload and \"JSESSIONID\" cookie is present" in {
        val successResponse = Future.successful((SuccessResponse(true), Seq.empty, Seq.empty, 200))
        when(accountService.forgotPasswordAction(any[Map[String, String]], any[ForgotPasswordRequest])).thenReturn(successResponse)

        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/forgot-password").withCookies(Cookie("JSESSIONID", "123")).withJsonBody(Json.toJson(ForgotPasswordRequest("incredibles@incredibles.com")))).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "response" \ "results" \ "success").as[Boolean] shouldBe (true)
      }

      "return with a status of " + BAD_REQUEST + " when given an invalid request in payload and \"JSESSIONID\" are present" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/forgot-password").withCookies(Cookie("JSESSIONID", "123")).withJsonBody(Json.toJson(ForgotPasswordRequest("abc235#abc235.com")))).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "errors" \\ "error")(0).as[String] shouldBe "email"
        (contentAsJson(result) \ "errors" \\ "data")(0).as[String] shouldBe "Enter a valid email address."
      }

    }

    "change password action" should {

      "return with a status of " + UNAUTHORIZED + "due to the absence of a \"JSESSIONID\" and \"USERNAME\" cookie" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/123/settings/change-password").withJsonBody(Json.toJson(ChangePasswordRequest("account1", "Password1@1", "Password1@1")))).get
        status(result) shouldBe UNAUTHORIZED
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + BAD_REQUEST + " due to the absence of a request body" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/123/settings/change-password").withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + BAD_REQUEST + " due to the absence of a request body and \"JSESSIONID\" and \"USERNAME\" cookie is present" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/123/settings/change-password").withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc")).withJsonBody(Json.obj())).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + OK + " when given a valid request in payload and \"JSESSIONID\" and \"USERNAME\" cookie is present" in {
        val successResponse = Future.successful((SuccessResponse(true), Seq.empty, Seq.empty, 200))
        when(accountService.changePasswordAction(any[Map[String, String]], any[ChangePasswordRequest])).thenReturn(successResponse)

        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/123/settings/change-password").withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc")).withJsonBody(Json.toJson(ChangePasswordRequest("account1", "passworD@11", "passworD@11")))).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "response" \ "results" \ "success").as[Boolean] shouldBe true
      }

      "return with a status of " + BAD_REQUEST + " when given an invalid request in payload and \"JSESSIONID\" and \"USERNAME\" are present" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/123/settings/change-password").withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc")).withJsonBody(Json.toJson(ChangePasswordRequest("", "pass", "Password1")))).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true

        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].length shouldBe 2
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]] foreach { error =>
          error.error match {
            case "new_password" =>
              error.data should be("Passwords must have at least 8 characters and contain all of the following: uppercase letters, lowercase letters, numbers, and symbols.")
            case "confirm_password" =>
              error.data should be("Passwords must have at least 8 characters and contain all of the following: uppercase letters, lowercase letters, numbers, and symbols.")
          }
        }
      }

      "return with a status of " + BAD_REQUEST + " when given mismatching passwords" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/123/settings/change-password").withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc")).withJsonBody(Json.toJson(ChangePasswordRequest("838#ahshhe", "passworD@1", "passworD@2")))).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true

        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].length shouldBe 1
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]] foreach { error =>
          error.error should be("confirm_password")
          error.data should be("Passwords must match.")
        }
      }
    }

    "account settings " should {

      val settingsSelectedSubscriptions = Seq(SaksOptStatus, Off5thOptStatus)
      val enabled = true
      val links = Some(Map("edit_action" -> "/v1/account-service/accounts/123/settings/email-preferences"))
      val account = UserAccount(Some("12345"), "mail@mail.com", "george", Some("92928383838"), "mail@mail.com", Some(""), "someName", Some("a uuid"))
      val passwordSettingsModel = PasswordSettingModel(true, None, None)

      s"return users preferences with status $OK for off5th banner for get account settings" in {

        val email_subscriptions = EmailSubscriptionsModelBuilder.buildUpdatePreferencesOptions(Banners.Off5th, settingsSelectedSubscriptions)
        val emailPreferencesModel = EmailPreferencesResponseModel(enabled, email_subscriptions, links, None)
        val successfulAccountResponse = Future.successful((AccountSettingsResponseModel(UserAccountWebsiteModel(true, account, Map.empty, None), passwordSettingsModel, emailPreferencesModel, Seq.empty, None), Seq.empty, Seq.empty, 200))
        when(accountService.getAccountSettings(any[Map[String, String]], any())).thenReturn(successfulAccountResponse)

        val eventualResult: Future[Result] = route(application, FakeRequest(GET, versionCtx + "/account-service/accounts/123/settings").withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get

        status(eventualResult) should be(OK)
        (contentAsJson(eventualResult) \ "errors").as[Seq[ApiErrorModel]] should be(Nil)
        (contentAsJson(eventualResult) \ "response" \ "results" \ "email_preferences" \ "enabled").as[Boolean] should be(true)
        (contentAsJson(eventualResult) \ "response" \ "results" \ "email_preferences" \ "email_subscriptions" \ "value").as[Seq[String]] should be(settingsSelectedSubscriptions)
        (contentAsJson(eventualResult) \ "response" \ "results" \ "email_preferences" \ "links" \ "edit_action").as[String] should be("/v1/account-service/accounts/123/settings/email-preferences")
      }

      s"return users preferences with status $OK for saks banner for get account settings" in {

        val email_preferences = EmailSubscriptionsModelBuilder.buildUpdatePreferencesOptions(Banners.Saks, settingsSelectedSubscriptions)
        val emailPreferencesModel = EmailPreferencesResponseModel(enabled, email_preferences, links, None)
        val successfulAccountResponse = Future.successful((AccountSettingsResponseModel(UserAccountWebsiteModel(true, account, Map.empty, None), passwordSettingsModel, emailPreferencesModel, Seq.empty, None), Seq.empty, Seq.empty, 200))
        when(accountService.getAccountSettings(any[Map[String, String]], any())).thenReturn(successfulAccountResponse)
        val eventualResult: Future[Result] = route(application, FakeRequest(GET, versionCtx + "/account-service/accounts/123/settings").withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(eventualResult) should be(OK)
        (contentAsJson(eventualResult) \ "errors").as[Seq[ApiErrorModel]] should be(Nil)
        (contentAsJson(eventualResult) \ "response" \ "results" \ "email_preferences" \ "enabled").as[Boolean] should be(true)
        (contentAsJson(eventualResult) \ "response" \ "results" \ "email_preferences" \ "email_subscriptions" \ "value").as[Seq[String]] should be(settingsSelectedSubscriptions)
        (contentAsJson(eventualResult) \ "response" \ "results" \ "email_preferences" \ "links" \ "edit_action").as[String] should be("/v1/account-service/accounts/123/settings/email-preferences")
      }

      s"return users preferences with status $BAD_REQUEST when invalid email marketing opt status values" in {
        val successfulAuthCall = Future.successful((Some(SignedInWebsiteModel(Some("12345"), None, "", "", None, "", None, None, AccountTitleObject("", ""))), Seq.empty, Seq.empty, 200))
        when(accountService.getAccount(any[Map[String, String]])).thenReturn(successfulAuthCall)
        val eventualResult: Future[Result] = route(application, FakeRequest(PUT, versionCtx + "/account-service/accounts/12345/settings/email-preferences")
          .withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))
          .withBody(Json.toJson(EmailPreferencesRequest(Seq("unknown_field"))))).get

        status(eventualResult) should be(BAD_REQUEST)
        (contentAsJson(eventualResult) \ "errors").as[Seq[ApiErrorModel]].size should be(1)
      }

      s"return user updated preference with status with valid email marketing opt status values $OK" in {
        val successfulAuthCall = Future.successful((Some(SignedInWebsiteModel(Some("12345"), None, "", "", None, "", None, None, AccountTitleObject("", ""))), Seq.empty, Seq.empty, 200))
        when(accountService.getAccount(any[Map[String, String]])).thenReturn(successfulAuthCall)
        val successfulPreferences = Future.successful((PreferencesResponseModel(Seq("off5th_opt_status")), Seq.empty, Seq.empty, 200))
        when(accountService.updateEmailPreferences(any[PreferencesRequestModel], any[Map[String, String]])).thenReturn(successfulPreferences)
        val eventualResult: Future[Result] = route(application, FakeRequest(PUT, versionCtx + "/account-service/accounts/12345/settings/email-preferences")
          .withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))
          .withBody(Json.toJson(EmailPreferencesRequest(Seq("off5th_opt_status"))))).get

        status(eventualResult) should be(OK)
        (contentAsJson(eventualResult) \ "response" \ "results" \ "preferences").as[Seq[String]] should be(Seq("off5th_opt_status"))
        (contentAsJson(eventualResult) \ "errors").as[Seq[ApiErrorModel]] should be(Nil)
      }
    }

    "reset password action" should {

      "return with a status of " + BAD_REQUEST + " due to the absence of a request body" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/reset-password?Loc=something").withJsonBody(Json.obj())).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + OK + " when given a valid request in payload" in {
        val successful = Future.successful((ResetPasswordWebsiteModel(Some(""), "", "", "", true, ResetPasswordTitleObject("", "")), Seq.empty, Seq.empty, 200))
        when(accountService.resetPasswordAction(any[Map[String, String]], any[ResetPasswordRequest], any[String])).thenReturn(successful)
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/reset-password?Loc=something").withJsonBody(Json.toJson(ResetPasswordRequest("passworD@1", "passworD@1")))).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "response" \ "results" \ "first_name") should not be Nil
      }

      "return with a status of " + BAD_REQUEST + " when given an invalid request in payload" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/reset-password?Loc=something").withJsonBody(Json.toJson(ResetPasswordRequest("", "Password#1")))).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].length shouldBe 1
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]] foreach { error =>
          error.error should be("password")
          error.data should be("Passwords must have at least 8 characters and contain all of the following: uppercase letters, lowercase letters, numbers, and symbols.")
        }
      }
    }

    "old sign out action" should {
      "return with a status of " + OK + " when given a valid request" in {
        val successful = Future.successful((Some(SignOutWebsiteModel(HomePageLink("link"))), Seq(Cookie("JSESSIONID", "123")), Seq.empty[ApiErrorModel], 200))
        when(accountService.logout(any[Map[String, String]])).thenReturn(successful)
        val result = route(application, FakeRequest(GET, versionCtx + "/account-service/account/logout").withHeaders(("Cookie", "UserName=user@user.com;JSESSIONID=hshsy263"))).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "response" \ "results" \ "links") should not be Nil
      }
    }

    "sign out action" should {
      "return with a status of " + OK + " when given a valid request" in {
        val successful = Future.successful((Some(SignOutWebsiteModel(HomePageLink("link"))), Seq(Cookie("JSESSIONID", "123")), Seq.empty[ApiErrorModel], 200))
        when(accountService.logout(any[Map[String, String]])).thenReturn(successful)
        val result = route(application, FakeRequest(GET, versionCtx + "/account-service/accounts/123/sign-out").withHeaders(("Cookie", "UserName=user@user.com;JSESSIONID=hshsy263"))).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "response" \ "results" \ "links") should not be Nil
      }
    }

    "create account page action" should {
      val result = route(application, FakeRequest(GET, versionCtx + "/account-service/accounts")).get

      "returns a json with email_subscriptions" in {
        (contentAsJson(result) \ "response" \ "results" \ "email_subscriptions") should not be Nil
      }

      "returns a json with messages" in {
        (contentAsJson(result) \ "response" \ "results" \ "messages") should not be Nil
      }

      "returns a json with links" in {
        (contentAsJson(result) \ "response" \ "results" \ "links") should not be Nil
      }

      "returns a json with canadian_customer" in {
        (contentAsJson(result) \ "response" \ "results" \ "canadian_customer") should not be Nil
      }
    }
  }
}
