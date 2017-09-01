package unit.service

import builders.requests.CreateAccountPayloadBuilder
import constants.Banners
import helpers.{ AccountHelper, ConfigHelper, TogglesHelper }
import models.servicemodel.{ More, UserAccount }
import models.website._
import models.{ ApiErrorModel, ApiRequestModel, FailureResponse, SuccessfulResponse }
import org.joda.time.DateTime
import org.mockito.Matchers.{ eq => eqTo, _ }
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }
import play.api.libs.json._
import play.api.mvc.Cookie
import services.{ HttpTransportService, _ }
import utils.NoOpStatsDClient
import utils.TestUtils._
import validations.Validator

import scala.concurrent.{ Future, TimeoutException }
import scala.language.postfixOps
import scala.util.Left

class UserAccountServiceSpec extends WordSpec
    with Matchers
    with MockitoSugar {

  private val injector = createIsolatedApplication().build().injector
  val accountHelper = injector.instanceOf[AccountHelper]
  val validator = injector.instanceOf[Validator]
  val headers: Map[String, String] = Map("JSESSIONID" -> "123")
  val fakeTogglesHelper: TogglesHelper = mock[TogglesHelper]

  def asApiResponse(data: JsValue, errors: JsValue): JsValue = {
    Json.obj("request" -> ApiRequestModel("", "", "", ""), "errors" -> errors, "response" -> Json.obj("results" -> data))
  }

  private val userAccountServiceUrl: String = ConfigHelper.getStringProp("data-service.user-account")

  "User Account Service" should {
    "Register Account Action" should {

      "return with expected model,cookies empty errors and 200 status code for successful Response and should call more-account-service" in {
        val rewardsService = mock[RewardsService]
        val configHelper = mock[ConfigHelper]
        when(configHelper.banner).thenReturn(Banners.Off5th)
        when(configHelper.getStringProp("data-service.user-account")).thenReturn(userAccountServiceUrl)

        when(rewardsService.viewOrRegisterMoreAccount(any(), any())).thenReturn(Future.successful(Right(SuccessfulResponse(mock[More]))))

        val httpTransportService = mock[HttpTransportService]

        //mock call to create account
        when(httpTransportService.postToService[CreateAccountRequest, UserAccount](any(), eqTo(Some("create_account")), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(SuccessfulResponse(new UserAccount(Some("123"), "", "", None, "", None, "", Some("a uuid"))))))

        //mock call to email marketing service
        when(httpTransportService.postToService[JsObject, Boolean](any(), eqTo(Some("profile/store-profile")), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(SuccessfulResponse(true))))

        val togglesHelper = mock[TogglesHelper]
        val emailMarketingService = mock[EmailMarketingService]
        when(togglesHelper.getWaitForMoreToggleState).thenReturn(Future.successful(true))

        val userAccountService: UserAccountService = new UserAccountService(httpTransportService, rewardsService, NoOpStatsDClient, togglesHelper, accountHelper, configHelper, emailMarketingService)

        val createAccount = userAccountService.createAccount(headers, new CreateAccountPayloadBuilder().build(), "0.0.0.0")
        whenReady(createAccount) {
          case (createAccountJson, cookies, errors, code) =>
            cookies.size should be(0)
            errors.size should be(0)
            code shouldBe 200
        }

        verify(rewardsService, times(1)).viewOrRegisterMoreAccount(any(), any())
      }

      "return with expected model,cookies empty errors and 200 status code for successful Response, and should NOT call with more-account-service" in {
        val rewardsService = mock[RewardsService]
        val configHelper = mock[ConfigHelper]
        when(configHelper.banner).thenReturn(Banners.Saks)
        when(configHelper.getStringProp("data-service.user-account")).thenReturn(userAccountServiceUrl)
        when(rewardsService.viewOrRegisterMoreAccount(any(), any())).thenReturn(Future.successful(Right(SuccessfulResponse(mock[More]))))

        val httpTransportService = mock[HttpTransportService]

        //mock call to create account
        when(httpTransportService.postToService[CreateAccountRequest, UserAccount](any(), eqTo(Some("create_account")), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(SuccessfulResponse(new UserAccount(Some("123"), "", "", None, "", None, "", Some("a uuid"))))))

        //mock call to email marketing service
        when(httpTransportService.postToService[JsObject, Boolean](any(), eqTo(Some("profile/store-profile")), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(SuccessfulResponse(true))))

        val togglesHelper = mock[TogglesHelper]
        when(togglesHelper.getWaitForMoreToggleState).thenReturn(Future.successful(false))

        val emailMarketingService = mock[EmailMarketingService]

        val userAccountService: UserAccountService = new UserAccountService(httpTransportService, rewardsService, NoOpStatsDClient, togglesHelper, accountHelper, configHelper, emailMarketingService)

        val createAccount = userAccountService.createAccount(headers, new CreateAccountPayloadBuilder().build(), "0.0.0.0")
        whenReady(createAccount) {
          case (createAccountJson, cookies, errors, code) =>
            cookies.size should be(0)
            errors.size should be(0)
            code shouldBe 200
        }
        verify(rewardsService, never()).viewOrRegisterMoreAccount(any(), any())
      }

      "return user account creation response even when more account service call fails" in {
        val fakeTogglesHelper: TogglesHelper = mock[TogglesHelper]
        when(fakeTogglesHelper.getWaitForMoreToggleState).thenReturn(Future.successful(true))

        val rewardsService = mock[RewardsService]
        when(rewardsService.viewOrRegisterMoreAccount(any(), any())).thenReturn(Future.failed(new TimeoutException("Client took too long to respond.")))

        val httpTransportService = mock[HttpTransportService]
        val emailMarketingService = mock[EmailMarketingService]

        //mock call to create account
        when(httpTransportService.postToService[CreateAccountRequest, UserAccount](any(), eqTo(Some("create_account")), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(SuccessfulResponse(new UserAccount(Some("123"), "", "first", None, "email@email.com", None, "last", Some("a uuid"))))))

        val userAccountService: UserAccountService = new UserAccountService(httpTransportService, rewardsService, NoOpStatsDClient, fakeTogglesHelper, accountHelper, ConfigHelper, emailMarketingService)
        whenReady(userAccountService.createAccount(headers, new CreateAccountPayloadBuilder().build(), "0.0.0.0")) { response =>

          response._1.first_name should be("first")
          response._1.last_name should be("last")
          response._1.email should be("email@email.com")
        }
      }
    }

    "Edit Profile Action" should {
      "return with expected model,cookies empty errors and 200 status code for successful Response" in {
        val httpTransportService = mock[HttpTransportService]
        val fakeTogglesHelper: TogglesHelper = mock[TogglesHelper]
        val emailMarketingService = mock[EmailMarketingService]

        val updateAccountRequest = new UpdateAccountRequest("first", "last", "email@email.com")
        val expectedUserAccount = new UserAccount(Some("12345"), "email@email.com", "first", None, "email@email.com", Some("middle"), "last", Some("a uuid"))
        val expectedMarketingResponse = new MarketingSignUpModel("email@email.com", DateTime.now(), "F")

        when(httpTransportService.postToService[UpdateAccountRequest, UserAccount](any(), eqTo(Some("update_account")), eqTo(updateAccountRequest), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(SuccessfulResponse(expectedUserAccount, Seq.empty[Cookie]))))

        when(httpTransportService.getFromService[UserAccount](any(), eqTo(Some("get_account")), any(), any())(any()))
          .thenReturn(Future.successful(Right(SuccessfulResponse(expectedUserAccount, Seq.empty[Cookie]))))

        when(emailMarketingService.updateEmailProfile(any(), any(), any())).thenReturn(Future.successful(Right(SuccessfulResponse(Seq(expectedMarketingResponse)))))

        val userAccountService: UserAccountService = new UserAccountService(httpTransportService, mock[RewardsService], NoOpStatsDClient, fakeTogglesHelper, accountHelper, ConfigHelper, emailMarketingService)
        whenReady(userAccountService.updateProfile(headers, updateAccountRequest)) {
          case (updatedAccount, cookies, errors, code) =>
            updatedAccount.profile should be(expectedUserAccount)
            cookies.isEmpty should be(true)
            errors.isEmpty should be(true)
            code shouldBe 200
        }
      }

      "return with expected model, cookies, errors and 400 status code for failure Response in update profile" in {
        val httpTransportService = mock[HttpTransportService]
        val fakeTogglesHelper: TogglesHelper = mock[TogglesHelper]
        val emailMarketingService = mock[EmailMarketingService]
        val updateAccountRequest = new UpdateAccountRequest("first", "last", "email@email.com")
        val expectedErrorResponse = Seq(ApiErrorModel("Error here", "error.error"))
        val expectedUserAccount = new UserAccount(Some("12345"), "email@email.com", "first", None, "email@email.com", Some("middle"), "last", Some("a uuid"))

        when(httpTransportService.getFromService[UserAccount](any(), eqTo(Some("get_account")), any(), any())(any()))
          .thenReturn(Future.successful(Right(SuccessfulResponse(expectedUserAccount, Seq.empty[Cookie]))))

        when(httpTransportService.postToService[UpdateAccountRequest, UserAccount](any(), eqTo(Some("update_account")), eqTo(updateAccountRequest), any(), any())(any(), any()))
          .thenReturn(Future.successful(Left(FailureResponse(expectedErrorResponse, 400))))

        when(emailMarketingService.updateEmailProfile(any(), any(), any())).thenReturn(Future.successful(Left(FailureResponse(expectedErrorResponse, 500))))

        val userAccountService: UserAccountService = new UserAccountService(httpTransportService, mock[RewardsService], NoOpStatsDClient, fakeTogglesHelper, accountHelper, ConfigHelper, emailMarketingService)
        whenReady(userAccountService.updateProfile(headers, updateAccountRequest)) {
          case (updatedAccount, cookies, errors, code) => {
            code shouldBe 400
            errors.length shouldBe 2
            errors.foreach(error => {
              error shouldBe expectedErrorResponse(0)
            })
            cookies.isEmpty shouldBe true
          }
        }
      }

      "return with expected model, cookies, errors and 200 status code for failure Response in email update" in {
        val httpTransportService = mock[HttpTransportService]
        val fakeTogglesHelper: TogglesHelper = mock[TogglesHelper]
        val emailMarketingService = mock[EmailMarketingService]

        val updateAccountRequest = new UpdateAccountRequest("first", "last", "email@email.com")
        val expectedErrorResponse = Seq(ApiErrorModel("Error here", "error.error"))
        val expectedUserAccount = new UserAccount(Some("12345"), "email@email.com", "first", None, "email@email.com", Some("middle"), "last", Some("a uuid"))
        when(httpTransportService.getFromService[UserAccount](any(), eqTo(Some("get_account")), any(), any())(any()))
          .thenReturn(Future.successful(Right(SuccessfulResponse(expectedUserAccount, Seq.empty[Cookie]))))

        when(httpTransportService.postToService[UpdateAccountRequest, UserAccount](any(), eqTo(Some("update_account")), eqTo(updateAccountRequest), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(SuccessfulResponse(expectedUserAccount, Seq.empty[Cookie]))))

        when(emailMarketingService.updateEmailProfile(any(), any(), any())).thenReturn(Future.successful(Left(FailureResponse(expectedErrorResponse, 500))))

        val userAccountService: UserAccountService = new UserAccountService(httpTransportService, mock[RewardsService], NoOpStatsDClient, fakeTogglesHelper, accountHelper, ConfigHelper, emailMarketingService)
        whenReady(userAccountService.updateProfile(headers, updateAccountRequest)) {
          case (updatedAccount, cookies, errors, code) => {
            updatedAccount.profile should be(expectedUserAccount)
            code shouldBe 200
            errors.length shouldBe 1
            errors.foreach(error => {
              error shouldBe expectedErrorResponse(0)
            })
            cookies.isEmpty shouldBe true
          }
        }
      }

      "return with 500 status code and errors if first call to retrieve account fails and other services should not be called" in {
        val httpTransportService = mock[HttpTransportService]
        val fakeTogglesHelper: TogglesHelper = mock[TogglesHelper]
        val emailMarketingService = mock[EmailMarketingService]

        val updateAccountRequest = new UpdateAccountRequest("first", "last", "email@email.com")
        val expectedErrorResponse = Seq(ApiErrorModel("Error here", "error.error"))

        when(httpTransportService.getFromService[UserAccount](any(), eqTo(Some("get_account")), any(), any())(any()))
          .thenReturn(Future.successful(Left(FailureResponse(expectedErrorResponse, 500))))

        val userAccountService: UserAccountService = new UserAccountService(httpTransportService, mock[RewardsService], NoOpStatsDClient, fakeTogglesHelper, accountHelper, ConfigHelper, emailMarketingService)
        whenReady(userAccountService.updateProfile(headers, updateAccountRequest)) {
          case (updatedAccount, cookies, errors, code) => {
            verify(httpTransportService, never()).postToService[UpdateAccountRequest, UserAccount](any(), eqTo(Some("update_account")), any(), eqTo(headers), any())(any(), any())
            verify(httpTransportService, never()).putToService[JsObject, Seq[MarketingSignUpModel]](any(), any(), any(), any(), any())(any(), any())
            code shouldBe 500
            errors.length shouldBe 1
            errors.foreach(error => {
              error shouldBe expectedErrorResponse(0)
            })
            cookies.isEmpty shouldBe true
          }
        }
      }
    }
  }
}

