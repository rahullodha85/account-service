package unit.helpers

import constants.Banners
import constants.Constants._
import helpers.{ AccountHelper, ConfigHelper }
import models.ApiErrorModel
import models.website.CreateAccountRequest
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterEach, Matchers, WordSpec }
import play.api.inject._
import utils.TestUtils._
import org.mockito.Mockito.{ reset, when }

class AccountHelperSpec extends WordSpec
    with MockitoSugar
    with Matchers
    with BeforeAndAfterEach {

  val helper = mock[ConfigHelper]
  val accountHelper = createIsolatedApplication()
    .overrides(bind[ConfigHelper].toInstance(helper))
    .build().injector.instanceOf[AccountHelper]

  override protected def beforeEach(): Unit = {
    reset(helper)
    when(helper.banner).thenReturn(Banners.Saks)
  }

  "AccountHelper" should {
    "update duplicate address error with global message key so that the UI can use it" in {

      val errors = Seq(ApiErrorModel("You already have this address in your address book.  Please enter a different address.", "DUPLICATE_ADDRESS"))
      val transformedErrors = accountHelper.getBrandSpecificErrorMessage(errors)

      transformedErrors.head.error should be("global_message.DUPLICATE_ADDRESS")
      transformedErrors.head.data should be("You already have this address in your address book. Please enter a different address.")
    }

    "map server error to global message" in {
      val errors = Seq(ApiErrorModel("some error occurred.", "blue_martini.server_error"))
      val transformedErrors = accountHelper.getBrandSpecificErrorMessage(errors)

      transformedErrors.head.error should be("global_message.error")
      transformedErrors.head.data should be("A technical error has occurred. Please refresh the page.")
    }

    "should properly detect if request originates from website/mobile" in {
      accountHelper.isFullSiteRequest(Map("X-Forwarded-Host" -> "http://web1-devslot2.saksdirect.com")) should be(true)
      accountHelper.isFullSiteRequest(Map("X-Forwarded-Host" -> "http://web1-devslot30.digital.hbc.com")) should be(true)

      accountHelper.isFullSiteRequest(Map("X-Forwarded-Host" -> "http://qa.saks.com")) should be(true)
      accountHelper.isFullSiteRequest(Map("X-Forwarded-Host" -> "http://qa.saksoff5th.com")) should be(true)

      accountHelper.isFullSiteRequest(Map("X-Forwarded-Host" -> "http://saksoff5th.com")) should be(true)
      accountHelper.isFullSiteRequest(Map("X-Forwarded-Host" -> "http://saks.com")) should be(true)

      accountHelper.isFullSiteRequest(Map("X-Forwarded-Host" -> "http://devslot30mobile.digital.hbc.com")) should be(false)
      accountHelper.isFullSiteRequest(Map("X-Forwarded-Host" -> "http://mobile-devslot2.saksdirect.com")) should be(false)

      accountHelper.isFullSiteRequest(Map("X-Forwarded-Host" -> "http://m.qa.saks.com")) should be(false)
      accountHelper.isFullSiteRequest(Map("X-Forwarded-Host" -> "http://m-qa.saksoff5th.com")) should be(false)

      accountHelper.isFullSiteRequest(Map("X-Forwarded-Host" -> "http://m.saksoff5th.com")) should be(false)
      accountHelper.isFullSiteRequest(Map("X-Forwarded-Host" -> "http://m.saks.com")) should be(false)

    }

    "should properly fallback to fullsite if X-Forwarded-Host is not found" in {
      accountHelper.isFullSiteRequest(Map("Host" -> "http://web1-devslot2.saksdirect.com")) should be(true)
      accountHelper.isFullSiteRequest(Map("Host" -> "http://web1-devslot30.digital.hbc.com")) should be(true)

      accountHelper.isFullSiteRequest(Map("Host" -> "http://qa.saks.com")) should be(true)
      accountHelper.isFullSiteRequest(Map("Host" -> "http://qa.saksoff5th.com")) should be(true)

      accountHelper.isFullSiteRequest(Map("Host" -> "http://saksoff5th.com")) should be(true)
      accountHelper.isFullSiteRequest(Map("Host" -> "http://saks.com")) should be(true)

      accountHelper.isFullSiteRequest(Map("Host" -> "http://devslot30mobile.digital.hbc.com")) should be(true)
      accountHelper.isFullSiteRequest(Map("Host" -> "http://mobile-devslot2.saksdirect.com")) should be(true)

      accountHelper.isFullSiteRequest(Map("Host" -> "http://m.qa.saks.com")) should be(true)
      accountHelper.isFullSiteRequest(Map("Host" -> "http://m-qa.saksoff5th.com")) should be(true)

      accountHelper.isFullSiteRequest(Map("Host" -> "http://m.saksoff5th.com")) should be(true)
      accountHelper.isFullSiteRequest(Map("Host" -> "http://m.saks.com")) should be(true)

    }

    "pass along upward any unexpected failure response into a generic left error response when in dev mode" in {
      when(helper.getBooleanProp("dev-mode")).thenReturn(true)

      val errorModel: ApiErrorModel = ApiErrorModel("data", "error")
      val specificErrorMessage = accountHelper.getBrandSpecificErrorMessage(Seq(errorModel), status = 503)

      specificErrorMessage should be(Seq(errorModel))
    }

    "forwards any error if status code is less than 500" in {
      val errorModel: ApiErrorModel = ApiErrorModel("data", "error")
      val specificErrorMessage = accountHelper.getBrandSpecificErrorMessage(Seq(errorModel), status = 401)

      specificErrorMessage should be(Seq(errorModel))
    }

    "return generic error if status code is 500 and above" in {
      val errorModel: ApiErrorModel = ApiErrorModel("data", "error")
      when(helper.getBooleanProp("dev-mode")).thenReturn(false)

      val specificErrorMessage = accountHelper.getBrandSpecificErrorMessage(Seq(errorModel), status = 500)

      specificErrorMessage should be(Seq(ApiErrorModel(GENERIC_ERROR_MESSAGE, GENERIC_ERROR)))
    }

    "return expected email preferences for create account request containing saks_opt_status:T and off5th_opt_status:T" in {
      val expectedEmailPreferences = ("T", "T", Option("F"), Option("F"))

      val createAccountRequest = CreateAccountRequest("first_name", "last_name", "pwd", "pwd", "test@test.com", Option("F"),
        Option(false), Option("T"), Option("T"), Option("1111111111"), Option("10080"), None)

      accountHelper.DefaultPreferences.defaultPreferences(createAccountRequest) should be(expectedEmailPreferences)

    }

    "return expected email preferences for create account request containing saks_opt_status:T and off5th_opt_status:F" in {
      val expectedEmailPreferences = ("T", "F", Option("F"), Option("F"))

      val createAccountRequest = CreateAccountRequest("first_name", "last_name", "pwd", "pwd", "test@test.com", Option("F"),
        Option(false), Option("T"), Option("F"), Option("1111111111"), Option("10080"), None)

      accountHelper.DefaultPreferences.defaultPreferences(createAccountRequest) should be(expectedEmailPreferences)
    }

    "return expected email preferences for create account request containing saks_opt_status:F and off5th_opt_status:T" in {
      val expectedEmailPreferences = ("F", "T", Option("F"), Option("F"))

      val createAccountRequest = CreateAccountRequest("first_name", "last_name", "pwd", "pwd", "test@test.com", Option("F"),
        Option(false), Option("F"), Option("T"), Option("1111111111"), Option("10080"), None)

      accountHelper.DefaultPreferences.defaultPreferences(createAccountRequest) should be(expectedEmailPreferences)
    }

    "return expected email preferences for create account request containing saks_opt_status:F and off5th_opt_status:F" in {
      val expectedEmailPreferences = ("F", "F", Option("F"), Option("F"))

      val createAccountRequest = CreateAccountRequest("first_name", "last_name", "pwd", "pwd", "test@test.com", Option("F"),
        Option(false), Option("F"), Option("F"), Option("1111111111"), Option("10080"), None)

      accountHelper.DefaultPreferences.defaultPreferences(createAccountRequest) should be(expectedEmailPreferences)
    }

    "return expected email preferences (ignores request canadian opt statuses) for create account request containing canadian flag:T and canadian opt in:F" in {
      val expectedEmailPreferences = ("F", "F", Option("F"), Option("F"))

      val createAccountRequest = CreateAccountRequest("first_name", "last_name", "pwd", "pwd", "test@test.com", Option("T"),
        Option(false), Option("T"), Option("T"), Option("1111111111"), Option("10080"), None)

      accountHelper.DefaultPreferences.defaultPreferences(createAccountRequest) should be(expectedEmailPreferences)
    }

    "return expected email preferences (ignores request canadian opt statuses) for create account request containing canadian flag:T and canadian opt in:T for saks banner" in {
      val expectedEmailPreferences = ("T", "F", Option("T"), Option("F"))

      val createAccountRequest = CreateAccountRequest("first_name", "last_name", "pwd", "pwd", "test@test.com", Option("T"),
        Option(true), Option("F"), Option("F"), Option("1111111111"), Option("10080"), None)

      when(helper.banner).thenReturn(Banners.Saks)
      accountHelper.DefaultPreferences.defaultPreferences(createAccountRequest) should be(expectedEmailPreferences)
    }

    "return expected email preferences (ignores request canadian opt statuses) for create account request containing canadian flag:T and canadian opt in:T for off5th banner" in {
      val expectedEmailPreferences = ("F", "T", Option("F"), Option("T"))

      val createAccountRequest = CreateAccountRequest("first_name", "last_name", "pwd", "pwd", "test@test.com", Option("T"),
        Option(true), Option("F"), Option("F"), Option("1111111111"), Option("10080"), None)

      when(helper.banner).thenReturn(Banners.Off5th)
      accountHelper.DefaultPreferences.defaultPreferences(createAccountRequest) should be(expectedEmailPreferences)
    }

    "return the new resource url as payment method if path is passed" in {
      val links: Option[Map[String, String]] = accountHelper.buildPaymentMethodLinks("new/path")

      links.get.get("payment_method_resource").get should be("new/path")
    }

    "return the saks first link url" in {
      val links: Option[Map[String, String]] = accountHelper.buildSaksFirstLinks(Map("test" -> "123"), "123")

      links.get.get("link_saksfirst_action").get should be("/v1/account-service/accounts/123/loyalty-program")
    }

    "return the new resource url as new cancel order url if path is passed" in {
      val links: Option[Map[String, String]] = accountHelper.buildCancelOrderLink("newPath")

      links.get.get("cancel_order_action").get should be("/v1/account-service/orders/")
    }

    "return the new resource url as new view more orders url if path is passed" in {
      val links: Option[Map[String, String]] = accountHelper.buildViewMoreOrderLinks("newPath")

      links.get.get("view_more_action").get should be("newPath")
    }

    "return the new resource url as account settings change password if path is passed" in {
      val links: Option[Map[String, String]] = accountHelper.buildPwdSettingsLinks("1234567")

      links.get.get("edit_action").get should be("/v1/account-service/accounts/1234567/settings/change-password")
    }

    "return the new resource url as account settings email prefs if path is passed" in {
      val links: Option[Map[String, String]] = accountHelper.buildEmailPreferencesLinks("1234567")

      links.get.get("edit_action").get should be("/v1/account-service/accounts/1234567/settings/email-preferences")
    }

    "not return Favorites tab if favorites toggle is disabled" in {
      val headerTabs = accountHelper.getHeader(false)

      headerTabs.exists(_.id == FAVORITES_HEADER_TAB_ID) should be(false)
    }

    "return Favorites tab if favorites toggle is enabled" in {
      val headerTabs = accountHelper.getHeader(true)

      headerTabs.exists(_.id == FAVORITES_HEADER_TAB_ID) should be(true)
    }

    "return the new resource url as forgot password url if new reset-password controller route was used" in {
      val links: Map[String, String] = accountHelper.buildForgotPasswordLinks

      links.get("forgot_password_action").get should be("/v1/account-service/accounts/forgot-password")
    }

    "return the new resource url as reset password url if new reset-password controller route was used" in {
      val links: Map[String, String] = accountHelper.buildPasswordResetLinks("test")

      links.get("reset_password_action").get should be("/v1/account-service/accounts/reset-password?Loc=test")
    }
  }

}
