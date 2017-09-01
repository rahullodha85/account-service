package unit.controllers

import fixtures.RequestFixtures
import helpers.TogglesHelper
import models.ApiErrorModel
import models.servicemodel.{ BeautyBox, BeautyBoxRequest, Emails, MemberInfo }
import models.website._
import org.mockito.Matchers.{ any, eq => eql }
import org.mockito.Mockito.{ mock => _, _ }
import org.scalatest.{ BeforeAndAfterEach, Matchers, WordSpec }
import play.api.inject._
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{ AccountService, PaymentMethodService, RewardsService }
import utils.TestUtils._

import scala.concurrent.Future

class RewardsControllerSpec extends WordSpec
    with Matchers
    with BeforeAndAfterEach
    with RequestFixtures {

  val rewardsService = mock[RewardsService]
  val accountService = mock[AccountService]
  val paymentMethodService = mock[PaymentMethodService]
  val togglesHelper = mock[TogglesHelper]
  val application = createIsolatedApplication()
    .overrides(bind[RewardsService].toInstance(rewardsService))
    .overrides(bind[AccountService].toInstance(accountService))
    .overrides(bind[PaymentMethodService].toInstance(paymentMethodService))
    .overrides(bind[TogglesHelper].toInstance(togglesHelper))
    .build()

  override def beforeEach(): Unit = {
    reset(rewardsService, accountService, paymentMethodService, togglesHelper)
  }

  "Rewards Controller Controller" should {
    "POST link saks first" should {
      val saksFirstRequest = LinkSaksFirstRequest("george", "66208", "10000453")

      "return with a status of " + UNAUTHORIZED + " when not sending a \"JSESSIONID\" Cookie" in {
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/123/loyalty-program")).get
        status(result) shouldBe UNAUTHORIZED
        contentType(result).get == "application/json" shouldBe true
        verify(paymentMethodService, times(0)).createPaymentMethod(any(), any(), any())
      }

      "return with a status of " + BAD_REQUEST + " due to the absence of a request body" in {
        when(accountService.getAccount(any())).thenReturn(Future.successful((Some(SignedInWebsiteModel(Some("123"), None, "", "", None, "", None, None, AccountTitleObject("", ""))), Seq.empty, Seq.empty, 200)))

        val saksFirstRequestJson: JsValue = Json.toJson(saksFirstRequest.copy(name = ""))
        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/123/loyalty-program").withJsonBody(saksFirstRequestJson).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
        verify(paymentMethodService, times(0)).createPaymentMethod(any(), any(), any())
      }

      "return with a status of " + OK + " when given a valid request in valid payload and \"JSESSIONID\" cookie is present" in {
        when(accountService.getAccount(any())).thenReturn(Future.successful((Some(SignedInWebsiteModel(Some("123"), None, "", "", None, "", None, None, AccountTitleObject("", ""))), Seq.empty, Seq.empty, 200)))

        val saksFirstRequestJson: JsValue = Json.toJson(saksFirstRequest)

        val rewardsWebsiteModel = RewardsWebsiteModel(enabled = true, "saksfirst", None, None, Some(SaksFirstModel(true, true, MemberInfo.empty)), None, None)
        when(rewardsService.linkSaksFirstAccount(any[Map[String, String]], eql(saksFirstRequest), any())).thenReturn(Future.successful(rewardsWebsiteModel, Seq.empty[Cookie], Seq.empty[ApiErrorModel], 200))

        val result = route(application, FakeRequest(POST, versionCtx + "/account-service/accounts/123/loyalty-program").withJsonBody(saksFirstRequestJson).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get

        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].isEmpty should be(true)
        (contentAsJson(result) \ "response" \ "results" \ "enabled").as[Boolean] shouldBe true
        verify(paymentMethodService, times(1)).createPaymentMethod(any(), any(), any())
      }
    }

    "Beauty email send" should {
      val beautyEmailRequest = BeautyBoxRequest(Seq(BeautyBox("BB01", "2414342341", "1234")), Emails(Seq("big@email.com"), Seq.empty, Seq.empty), "beauty_box")

      "return with a status of " + OK + " when given a valid request in valid payload and \"JSESSIONID\" cookie is present" in {
        when(accountService.getAccount(any())).thenReturn(Future.successful((Some(SignedInWebsiteModel(Some("123"), None, "", "", None, "", None, None, AccountTitleObject("", ""))), Seq.empty, Seq.empty, 200)))

        val beautyEmailRequestJson: JsValue = Json.toJson(beautyEmailRequest)

        when(rewardsService.sendBeautyBoxEmail(any[Map[String, String]], any(), eql(beautyEmailRequest))).thenReturn(Future.successful(Some("Success!"), Seq.empty[Cookie], Seq.empty[ApiErrorModel], 200))
        when(rewardsService.getBeautyBoxRequestForUser(any[Map[String, String]], any[String], any[String])).thenReturn(Future.successful(Right(beautyEmailRequest)))

        val result = route(application, FakeRequest(PUT, versionCtx + "/account-service/accounts/123/loyalty-program/beauty/email").withJsonBody(beautyEmailRequestJson).withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get

        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
        (contentAsJson(result) \ "errors").as[Seq[ApiErrorModel]].isEmpty should be(true)
      }
    }

    "Get saks first" should {
      "call account service to make sure you're logged in" in {
        when(accountService.getAccount(any())).thenReturn(Future.successful((Some(SignedInWebsiteModel(Some("123"), None, "", "", None, "", None, None, AccountTitleObject("", ""))), Seq.empty, Seq.empty, 200)))

        when(togglesHelper.saksFirstSummaryEnabled).thenReturn(Future.successful(true))
        when(rewardsService.getSaksFirstAccountSummary(any[Map[String, String]], any())).thenReturn(Future.successful(None, Seq.empty[Cookie], Seq.empty[ApiErrorModel], 200))

        val result = route(application, FakeRequest(GET, versionCtx + "/account-service/accounts/123/loyalty-program").withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get

        status(result) shouldBe OK
        verify(accountService, times(1)).getAccount(any())
      }

      "not call account service to make sure you're logged in if the summary toggle is off and return enabled false" in {
        when(togglesHelper.saksFirstSummaryEnabled).thenReturn(Future.successful(false))

        val result = route(application, FakeRequest(GET, versionCtx + "/account-service/accounts/123/loyalty-program").withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get

        status(result) shouldBe OK
        (contentAsJson(result) \ "response" \ "results" \ "enabled").as[Boolean] should be(false)
        verify(accountService, never()).getAccount(any())
      }
    }
  }
}
