package unit.service

import helpers.{ AccountHelper, ConfigHelper, TogglesHelper }
import messages.HBCMessagesApi
import models.{ ApiErrorModel, FailureResponse, SuccessfulResponse, servicemodel }
import models.servicemodel._
import models.website.{ LinkSaksFirstRequest, RewardsWebsiteModel, SaksFirstModel, SaksFirstResponse }
import org.mockito.Matchers.{ eq => matchEqual, _ }
import org.mockito.Mockito.{ verify, when }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ ShouldMatchers, WordSpec }
import play.api.mvc.Cookie
import services.{ HttpTransportService, RewardsService }
import utils.NoOpStatsDClient
import utils.TestUtils._

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.language.postfixOps
import scala.util.{ Left, Right }

class RewardsServiceSpec extends WordSpec with ShouldMatchers with ScalaFutures {
  val more = More("8282728")
  val account = UserAccount(Some("12345"), "mail@mail.com", "george", Some("92928383838"), "mail@mail.com", Some(""), "someName", Some("a uuid"))
  val headers: Map[String, String] = Map("JSESSIONID" -> "123")
  val successResponse = SuccessfulResponse(account, Seq.empty[Cookie])
  val successfulAccountResponse = Right(successResponse)
  val successfulMoreResponse = Future.successful(Right(SuccessfulResponse(more)))

  "A RewardsService" should {
    "retrieve more using http when WAIT FOR MORE toggle is OFF" in {
      val togglesHelper: TogglesHelper = mock[TogglesHelper]
      val httpTransportService: HttpTransportService = mock[HttpTransportService]
      val configHelper = mock[ConfigHelper]
      val accountHelper = mock[AccountHelper]

      when(configHelper.getIntProp("more.service.timeout")).thenReturn(1)
      when(togglesHelper.getWaitForMoreToggleState).thenReturn(Future.successful(false))
      when(httpTransportService.postToService[MoreRequest, More](any(), matchEqual(Some("verifyAccount")), any(), any(), any())(any(), any())).thenReturn(successfulMoreResponse)

      val rewardsService = new RewardsService(httpTransportService, togglesHelper, NoOpStatsDClient, configHelper, accountHelper, mock[HBCMessagesApi])

      Await.result(rewardsService.retrieveMoreAccount(successResponse.body, headers), 2 seconds)
      verify(httpTransportService).postToService[MoreRequest, More](any(), matchEqual(Some("verifyAccount")), any(), any(), any())(any(), any())
    }

    "view or register for more using http when WAIT FOR MORE toggle is ON" in {
      val togglesHelper: TogglesHelper = mock[TogglesHelper]
      val httpTransportService: HttpTransportService = mock[HttpTransportService]
      val accountHelper = mock[AccountHelper]

      val configHelper = mock[ConfigHelper]
      when(configHelper.getIntProp("more.service.timeout")).thenReturn(1)
      when(togglesHelper.getWaitForMoreToggleState).thenReturn(Future.successful(true))
      when(httpTransportService.postToService[MoreRequest, More](any(), matchEqual(Some("getOrRegister")), any(), any(), any())(any(), any())).thenReturn(successfulMoreResponse)

      val rewardsService = new RewardsService(httpTransportService, togglesHelper, NoOpStatsDClient, configHelper, accountHelper, mock[HBCMessagesApi])

      Await.result(rewardsService.retrieveMoreAccount(successResponse.body, headers), 2 seconds)
      verify(httpTransportService).postToService[MoreRequest, More](any(), matchEqual(Some("getOrRegister")), any(), any(), any())(any(), any())
    }

    "get beauty boxes for user" in {
      val httpTransportService = mock[HttpTransportService]
      val expectedResponse: Future[Either[FailureResponse, SuccessfulResponse[SaksFirstSummaryPage]]] =
        Future.successful(Right(SuccessfulResponse(SaksFirstSummaryPage(true, true, true, new MemberInfo(true, "12345678", "100", "Premier", "", "", 2400, 0),
          Some(Seq(GiftCardInfo("1234", "$20"))), None, Some(SaksFirstBeauty("", Some(Seq(BeautyBox("BB01", "2414342341", "1234"))))), None, None, None))))
      val accountHelper = mock[AccountHelper]
      val togglesHelper: TogglesHelper = mock[TogglesHelper]
      val configHelper = mock[ConfigHelper]

      val rewardsService = new RewardsService(httpTransportService, togglesHelper, NoOpStatsDClient, configHelper, accountHelper, mock[HBCMessagesApi])

      when(httpTransportService.getFromService[SaksFirstSummaryPage](any(), matchEqual(Some("accounts/123/loyalty-program/summary")), any(), any())(any())).thenReturn(expectedResponse)

      rewardsService.getBeautyBoxRequestForUser(headers, "123", "test@email.test").map {
        maybeBeautyBoxRequest =>
          {
            maybeBeautyBoxRequest should be(Right(BeautyBoxRequest))

            maybeBeautyBoxRequest match {
              case Right(beautyBoxRequest) => {
                beautyBoxRequest.event should be("beauty-box")
                beautyBoxRequest.boxes should be(Seq(BeautyBox("BB01", "2414342341", "1234")))
                beautyBoxRequest.email_addresses should be(Emails(List("test@email.test"), List(), List()))
              }
            }
          }
      }(scala.concurrent.ExecutionContext.Implicits.global)
    }
  }

  "Link saks first" should {
    "return with expected model,cookies empty errors and 200 status code for successful Response" in {
      val httpTransportService = mock[HttpTransportService]
      val saksFirstRequest = LinkSaksFirstRequest("george lopez", "66208", "10000453")
      val saksFirstResponse: SaksFirstResponse = SaksFirstResponse(true, MemberInfo.empty)
      val expectedResponse: Future[Either[FailureResponse, SuccessfulResponse[SaksFirstResponse]]] = Future.successful(Right(SuccessfulResponse(saksFirstResponse)))
      val accountHelper = mock[AccountHelper]
      val togglesHelper: TogglesHelper = mock[TogglesHelper]

      when(togglesHelper.migratingSaksFirst).thenReturn(Future.successful(false))
      when(httpTransportService.postToService[LinkSaksFirstRequest, SaksFirstResponse](any(), matchEqual(Some("accounts/123/loyalty-program")), matchEqual(saksFirstRequest), any(), any())(any(), any())).thenReturn(expectedResponse)
      val rewardsService = new RewardsService(httpTransportService, togglesHelper, NoOpStatsDClient, mock[ConfigHelper], accountHelper, mock[HBCMessagesApi])

      whenReady(rewardsService.linkSaksFirstAccount(headers, saksFirstRequest, "123")) {
        case (rewardsResponse, cookies, errors, code) =>
          rewardsResponse.enabled should be(true)
          cookies.isEmpty should be(true)
          errors.isEmpty should be(true)
          code shouldBe 200
      }
    }

    "return success false for a failure response along with cookies and headers" in {
      val expectedResponse = Future.successful(Left(FailureResponse(Seq(ApiErrorModel("Some error", "error.error")), 400)))
      val httpTransportService = mock[HttpTransportService]
      val saksFirstRequest = LinkSaksFirstRequest("george", "66208", "10000453")
      val accountHelper = mock[AccountHelper]
      val togglesHelper: TogglesHelper = mock[TogglesHelper]

      when(togglesHelper.migratingSaksFirst).thenReturn(Future.successful(false))
      when(httpTransportService.postToService[LinkSaksFirstRequest, SuccessResponse](any(), matchEqual(Some("accounts/123/loyalty-program")), matchEqual(saksFirstRequest), any(), any())(any(), any())).thenReturn(expectedResponse)

      val rewardsService = new RewardsService(httpTransportService, togglesHelper, NoOpStatsDClient, mock[ConfigHelper], accountHelper, mock[HBCMessagesApi])

      whenReady(rewardsService.linkSaksFirstAccount(headers, saksFirstRequest, "123")) {
        case (rewardsResponse, cookies, errors, code) =>
          rewardsResponse.enabled should be(true)
          cookies.isEmpty should be(true)
          errors.isEmpty should be(false)
          code shouldBe 400
      }
    }

    "return with SaksFirst model and 200 status code for successful Response" in {
      val httpTransportService = mock[HttpTransportService]
      val expectedResponse: Future[Either[FailureResponse, SuccessfulResponse[SaksFirstAccountSummary]]] = Future.successful(Right(SuccessfulResponse(SaksFirstAccountSummary(true, new MemberInfo(true, "12345678", "100", "Premier", "", "", 2400, 0), None, None))))
      val accountHelper = mock[AccountHelper]

      when(httpTransportService.getFromService[SaksFirstAccountSummary](any(), matchEqual(Some("accounts/123/loyalty-program")), any(), any())(any())).thenReturn(expectedResponse)
      val rewardsService = new RewardsService(httpTransportService, mock[TogglesHelper], NoOpStatsDClient, mock[ConfigHelper], accountHelper, mock[HBCMessagesApi])

      whenReady(rewardsService.getSaksFirstAccountSummary(headers, "123")) {
        case (Some(saksFirstInfo), _, _, code) =>
          saksFirstInfo.enabled should be(true)
          saksFirstInfo.member_info.available_points should be("100")
          code shouldBe 200
      }
    }

    "return with PointsInfo model and 200 status code for successful Response" in {
      val httpTransportService = mock[HttpTransportService]
      val expectedResponse: Future[Either[FailureResponse, SuccessfulResponse[SaksFirstSummaryPage]]] = Future.successful(Right(SuccessfulResponse(SaksFirstSummaryPage(true, true, true, new MemberInfo(true, "12345678", "100", "Premier", "", "", 2400, 0), Some(Seq(GiftCardInfo("1234", "$20"))), None, None, None, None, None))))
      val accountHelper = mock[AccountHelper]

      when(httpTransportService.getFromService[SaksFirstSummaryPage](any(), matchEqual(Some("accounts/123/loyalty-program/summary")), any(), any())(any())).thenReturn(expectedResponse)
      val helper = mock[TogglesHelper]
      when(helper.saksFirstPageEnabled).thenReturn(Future.successful(true))
      when(helper.disableRedemption).thenReturn(Future.successful(false))
      when(helper.beautyEnabled).thenReturn(Future.successful(true))
      val rewardsService = new RewardsService(httpTransportService, helper, NoOpStatsDClient, mock[ConfigHelper], accountHelper, mock[HBCMessagesApi])

      whenReady(rewardsService.getSaksFirstSummaryPage(headers, "123")) {
        case (pointsInfo, _, _, code) =>
          pointsInfo.enabled should be(true)
          pointsInfo.enabled_beauty should be(true)
          pointsInfo.member_info.available_points should be("100")
          pointsInfo.gift_card_history.get.head.card_number should be("1234")
          code shouldBe 200
      }
    }

    "return beauty enabled if toggle is on and response from user-account-service is beauty enabled true" in {
      val httpTransportService = mock[HttpTransportService]
      val expectedResponse: Future[Either[FailureResponse, SuccessfulResponse[SaksFirstSummaryPage]]] = Future.successful(Right(SuccessfulResponse(SaksFirstSummaryPage(true, true, true, new MemberInfo(true, "12345678", "100", "Premier", "", "", 2400, 0), Some(Seq(GiftCardInfo("1234", "$20"))), None, None, None, None, None))))
      val accountHelper = mock[AccountHelper]

      when(httpTransportService.getFromService[SaksFirstSummaryPage](any(), matchEqual(Some("accounts/123/loyalty-program/summary")), any(), any())(any())).thenReturn(expectedResponse)
      val helper = mock[TogglesHelper]
      when(helper.saksFirstPageEnabled).thenReturn(Future.successful(true))
      when(helper.disableRedemption).thenReturn(Future.successful(false))
      when(helper.beautyEnabled).thenReturn(Future.successful(true))
      val rewardsService = new RewardsService(httpTransportService, helper, NoOpStatsDClient, mock[ConfigHelper], accountHelper, mock[HBCMessagesApi])

      whenReady(rewardsService.getSaksFirstSummaryPage(headers, "123")) {
        case (pointsInfo, _, _, code) =>
          pointsInfo.enabled_beauty should be(true)
      }
    }

    "return beauty disabled if toggle is off but response from user-account-service is beauty enabled true" in {
      val httpTransportService = mock[HttpTransportService]
      val expectedResponse: Future[Either[FailureResponse, SuccessfulResponse[SaksFirstSummaryPage]]] = Future.successful(Right(SuccessfulResponse(SaksFirstSummaryPage(true, true, true, new MemberInfo(true, "12345678", "100", "Premier", "", "", 2400, 0), Some(Seq(GiftCardInfo("1234", "$20"))), None, None, None, None, None))))
      val accountHelper = mock[AccountHelper]

      when(httpTransportService.getFromService[SaksFirstSummaryPage](any(), matchEqual(Some("accounts/123/loyalty-program/summary")), any(), any())(any())).thenReturn(expectedResponse)
      val helper = mock[TogglesHelper]
      when(helper.saksFirstPageEnabled).thenReturn(Future.successful(true))
      when(helper.disableRedemption).thenReturn(Future.successful(false))
      when(helper.beautyEnabled).thenReturn(Future.successful(false))
      val rewardsService = new RewardsService(httpTransportService, helper, NoOpStatsDClient, mock[ConfigHelper], accountHelper, mock[HBCMessagesApi])

      whenReady(rewardsService.getSaksFirstSummaryPage(headers, "123")) {
        case (pointsInfo, _, _, code) =>
          pointsInfo.enabled_beauty should be(false)
      }
    }

    "return beauty disabled if toggle is on but response from user-account-service is beauty enabled false" in {
      val httpTransportService = mock[HttpTransportService]
      val expectedResponse: Future[Either[FailureResponse, SuccessfulResponse[SaksFirstSummaryPage]]] = Future.successful(Right(SuccessfulResponse(SaksFirstSummaryPage(true, true, false, new MemberInfo(true, "12345678", "100", "Premier", "", "", 2400, 0), Some(Seq(GiftCardInfo("1234", "$20"))), None, None, None, None, None))))
      val accountHelper = mock[AccountHelper]

      when(httpTransportService.getFromService[SaksFirstSummaryPage](any(), matchEqual(Some("accounts/123/loyalty-program/summary")), any(), any())(any())).thenReturn(expectedResponse)
      val helper = mock[TogglesHelper]
      when(helper.saksFirstPageEnabled).thenReturn(Future.successful(true))
      when(helper.disableRedemption).thenReturn(Future.successful(false))
      when(helper.beautyEnabled).thenReturn(Future.successful(true))
      val rewardsService = new RewardsService(httpTransportService, helper, NoOpStatsDClient, mock[ConfigHelper], accountHelper, mock[HBCMessagesApi])

      whenReady(rewardsService.getSaksFirstSummaryPage(headers, "123")) {
        case (pointsInfo, _, _, code) =>
          pointsInfo.enabled_beauty should be(false)
      }
    }

    "return a generic linking failure if zip code fails to match" in {
      val httpTransportService = mock[HttpTransportService]
      val expectedResponse = Future.successful(Left(FailureResponse(Seq(ApiErrorModel("zip", "zip code is wrong")), 400, Seq.empty)))
      val accountHelper = mock[AccountHelper]
      val togglesHelper: TogglesHelper = mock[TogglesHelper]

      when(togglesHelper.migratingSaksFirst).thenReturn(Future.successful(false))
      when(httpTransportService.postToService[LinkSaksFirstRequest, SuccessResponse](any(), matchEqual(Some("accounts/123/loyalty-program")), any(), any(), any())(any(), any())).thenReturn(expectedResponse)
      val rewardsService = new RewardsService(httpTransportService, togglesHelper, NoOpStatsDClient, mock[ConfigHelper], accountHelper, mock[HBCMessagesApi])

      whenReady(rewardsService.linkSaksFirstAccount(headers, new LinkSaksFirstRequest("howdy man", "10008", "567"), "123")) {
        case (_, _, errors, code) =>
          code shouldBe 400
          errors.head.error shouldBe "global_message.saks_first_number"
      }
    }

    "return a generic linking failure if saks first number fails to match" in {
      val httpTransportService = mock[HttpTransportService]
      val expectedResponse = Future.successful(Left(FailureResponse(Seq(ApiErrorModel("saks_first_number", "card number is wrong")), 400, Seq.empty)))
      val accountHelper = mock[AccountHelper]
      val togglesHelper: TogglesHelper = mock[TogglesHelper]

      when(togglesHelper.migratingSaksFirst).thenReturn(Future.successful(false))
      when(httpTransportService.postToService[LinkSaksFirstRequest, SuccessResponse](any(), matchEqual(Some("accounts/123/loyalty-program")), any(), any(), any())(any(), any())).thenReturn(expectedResponse)
      val rewardsService = new RewardsService(httpTransportService, togglesHelper, NoOpStatsDClient, mock[ConfigHelper], accountHelper, mock[HBCMessagesApi])

      whenReady(rewardsService.linkSaksFirstAccount(headers, new LinkSaksFirstRequest("howdy man", "10008", "567"), "123")) {
        case (_, _, errors, code) =>
          code shouldBe 400
          errors.head.error shouldBe "global_message.saks_first_number"
      }
    }
  }
}
