package unit.controllers

import fixtures.RequestFixtures
import models.ApiErrorModel
import models.servicemodel.UserAccount
import models.website._
import org.mockito.Matchers.{ any, eq => eql }
import org.mockito.Mockito.{ mock => _, _ }
import org.scalatest.{ BeforeAndAfterEach, Matchers, WordSpec }
import play.api.inject._
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.UserAccountService
import utils.TestUtils._

import scala.concurrent.Future

class UserAccountControllerSpec extends WordSpec
    with Matchers
    with BeforeAndAfterEach
    with RequestFixtures {

  val userAccountService = mock[UserAccountService]
  val application = createIsolatedApplication()
    .overrides(bind[UserAccountService].toInstance(userAccountService))
    .build()

  override def beforeEach(): Unit = {
    reset(userAccountService)
  }

  "User Account Controller" should {

    "create account action" should {

      "return with a status of " + BAD_REQUEST + "  due to the absence of a payload" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts")).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].nonEmpty should be(true)
      }

      "return with a status of " + OK + " when given a valid request in payload" in {
        val accountRequest = CreateAccountResponseWebsiteModel(Some("123"), "", "test", "user", None, "", AccountTitleObject("", ""))
        val success = Future.successful(accountRequest, Seq.empty, Seq.empty, 200)
        when(userAccountService.createAccount(any[Map[String, String]], any[CreateAccountRequest], any[String])).thenReturn(success)

        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts").withJsonBody(validRegisterPayload)).get

        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "response" \ "results" \ "first_name").as[String] shouldBe "test"
        (contentAsJson(result) \ "response" \ "results" \ "last_name").as[String] shouldBe "user"
      }

      "return with a status of " + BAD_REQUEST + " when given a valid request in payload" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts").withJsonBody(invalidRegisterPayload)).get

        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].size should be(4)
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]] foreach { error =>
          error.error match {
            case "last_name" =>
              error.data should be("Please enter a valid last name.")
            case "password" =>
              error.data should be("Passwords must have at least 8 characters and contain all of the following: uppercase letters, lowercase letters, numbers, and symbols.")
            case "first_name" =>
              error.data should be("Please enter a valid first name.")
            case "email" =>
              error.data should be("Enter a valid email address.")
          }
        }
      }
    }

    "update profile action" should {

      "return with a status of " + UNAUTHORIZED + " when not sending a \"JSESSIONID\" Cookie" in {
        val result = route(application, FakeRequest(PUT, versionCtx + "/account-service/accounts/123").withJsonBody(validUpdateProfilePayload)).get
        status(result) shouldBe UNAUTHORIZED
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + BAD_REQUEST + " due to the absence of a request body" in {
        val result = route(application, FakeRequest(PUT, versionCtx + "/account-service/accounts/123").withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + OK + " when given a valid request in valid payload and \"JSESSIONID\" cookie is present" in {
        val success = Future.successful(UpdateProfileResponseWebsiteModel(UserAccount(Some("12345"), "", "test", None, "", None, "", Some("a uuid"))), Seq.empty, Seq.empty, 200)
        when(userAccountService.updateProfile(any[Map[String, String]], any[UpdateAccountRequest])).thenReturn(success)

        val result = route(application, FakeRequest(PUT, versionCtx + "/account-service/accounts/123").withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc")).withJsonBody(validUpdateProfilePayload)).get
        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].isEmpty should be(true)
        (contentAsJson(result) \ "response" \ "results" \ "profile" \ "first_name").as[String] shouldBe "test"
      }

    }
  }
}
