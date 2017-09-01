package services

import javax.inject.Inject

import akka.pattern.after
import constants.Constants
import constants.Constants._
import models.Constants._
import globals.actorSystem
import helpers.{ AccountHelper, ConfigHelper, TogglesHelper }
import messages.HBCMessagesApi
import models.servicemodel._
import models.website._
import models.{ ApiErrorModel, ApiResultModel, FailureResponse, SuccessfulResponse }
import monitoring.StatsDClientLike
import play.Logger
import play.api.i18n.{ I18nSupport, Messages }
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.Cookie

import scala.concurrent.duration._
import scala.concurrent.{ Future, TimeoutException }
import scala.language.postfixOps

class RewardsService @Inject() (
    httpTransportService: HttpTransportService,
    togglesHelper:        TogglesHelper,
    statsDClient:         StatsDClientLike,
    configHelper:         ConfigHelper,
    accountHelper:        AccountHelper,
    val messagesApi:      HBCMessagesApi
) extends I18nSupport {
  import play.api.libs.concurrent.Execution.Implicits._
  val moreServiceTimeoutDuration = configHelper.getIntProp("more.service.timeout")
  val moreServiceUrl = configHelper.getStringProp("data-service.more-account")
  val userAccountServiceUrl: String = configHelper.getStringProp("data-service.user-account")
  val customerNotificationServiceUrl: String = configHelper.getStringProp("data-service.customer-notification")

  def viewOrRegisterMoreAccount(createAccountRequest: CreateAccountRequest, headers: Map[String, String]): Future[Either[FailureResponse, SuccessfulResponse[More]]] = {
    val registerMoreRequest: MoreRequest = MoreRequest(Some(createAccountRequest.first_name), createAccountRequest.last_name, createAccountRequest.email, createAccountRequest.phone_number.getOrElse(""), createAccountRequest.zip)
    viewOrRegisterMoreAccount(headers, registerMoreRequest)
  }

  def retrieveMoreAccount(userAccount: UserAccount, headers: Map[String, String]): Future[Either[FailureResponse, SuccessfulResponse[More]]] = {
    for {
      waitForMoreToggle <- togglesHelper.getWaitForMoreToggleState
      response <- if (waitForMoreToggle) {
        Logger.info(s"${Constants.TOGGLE_WAIT_FOR_MORE_NUMBER} is on, Executing a 'verify or register call' to register for a more account if user does not have one already.")
        viewOrRegisterMoreAccount(headers, MoreRequest(Some(userAccount.first_name), userAccount.last_name, userAccount.email, userAccount.phone_number.getOrElse(""), Some("")))
      } else {
        viewMoreAccount(headers, MoreRequest(last_name = userAccount.last_name, email_address = userAccount.email, phone_number = userAccount.phone_number.getOrElse("")))
      }
    } yield response
  }

  private def viewOrRegisterMoreAccount(headers: Map[String, String], registerMoreRequest: MoreRequest): Future[Either[FailureResponse, SuccessfulResponse[More]]] = {
    val eventualMoreResponse = statsDClient.time("RewardsService.viewOrRegisterMoreAccount") {
      httpTransportService.postToService[JsObject, More](moreServiceUrl, Some("getOrRegister"), Json.obj("item" -> Json.toJson[MoreRequest](registerMoreRequest)), headers)
    }
    timingOutIn(eventualMoreResponse, moreServiceTimeoutDuration seconds)
  }

  private def viewMoreAccount(headers: Map[String, String], verifyMoreReq: MoreRequest): Future[Either[FailureResponse, SuccessfulResponse[More]]] = {
    val eventualMoreResponse = statsDClient.time("RewardsService.viewMoreAccount") {
      httpTransportService.postToService[JsObject, More](moreServiceUrl, Some("verifyAccount"), Json.obj("item" -> Json.toJson[MoreRequest](verifyMoreReq)), headers)
    }
    timingOutIn(eventualMoreResponse, moreServiceTimeoutDuration seconds)
  }

  def linkSaksFirstAccount(headers: Map[String, String], linkSaksFirstRequest: LinkSaksFirstRequest, accountId: String): Future[(RewardsWebsiteModel, Seq[Cookie], Seq[ApiErrorModel], Int)] = {

    for {
      migrating <- togglesHelper.migratingSaksFirst
      response <- if (migrating) {
        val errors = Seq(accountHelper.migratingSaksFirstError)
        Future.successful(RewardsWebsiteModel(enabled = false, "saksfirst", None, None, Some(SaksFirstModel(false, true, MemberInfo.empty)), None, None), Seq.empty, errors, 400)
      } else {
        statsDClient.time("AccountService.linkSaksFirstAccount") {
          httpTransportService.postToService[LinkSaksFirstRequest, SaksFirstResponse](userAccountServiceUrl, Some(s"accounts/$accountId/loyalty-program"), linkSaksFirstRequest, headers).map {
            case Left(failureResponse) =>
              (RewardsWebsiteModel(enabled = true, "saksfirst", None, None, None, None, None), failureResponse.cookies, genericizeLinkingError(failureResponse.errors), failureResponse.code)
            case Right(successfulResponse) =>
              val rewardsWebsiteModel = RewardsWebsiteModel(enabled = true, "saksfirst", Some(SAKS_FIRST_REWARDS_THRESHOLD), None, Some(SaksFirstModel(
                successfulResponse.body.member_info.linked,
                successfulResponse.body.enabled, successfulResponse.body.member_info
              )), None, None)
              (rewardsWebsiteModel, successfulResponse.cookies, Seq.empty[ApiErrorModel], 200)
          }
        }
      }
    } yield response
  }

  def genericizeLinkingError(errors: Seq[ApiErrorModel]): Seq[ApiErrorModel] = {
    errors.map { error =>
      val genericLinkingFailureMessage = ApiErrorModel(Messages(SAKS_FIRST_LINKING_ERROR), "global_message.saks_first_number")
      error.data match {
        case "zip"               => genericLinkingFailureMessage
        case "saks_first_number" => genericLinkingFailureMessage
        case _                   => error
      }
    }
  }

  def getSaksFirstAccountSummary(headers: Map[String, String], accountId: String): Future[(Option[SaksFirstAccountSummary], Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("AccountService.getSaksFirstInfo")({
      httpTransportService.getFromService[SaksFirstAccountSummary](userAccountServiceUrl, Some(s"accounts/$accountId/loyalty-program"), headers).map {
        case Left(failureResponse) => (None, failureResponse.cookies, failureResponse.errors, failureResponse.code)
        case Right(successfulResponse) =>
          val summary = SaksFirstAccountSummary(successfulResponse.body.enabled, successfulResponse.body.member_info, accountHelper.buildSaksFirstSummaryMessages,
            accountHelper.buildSaksFirstSummaryLinks(headers, Some(accountId)))
          (Some(summary), successfulResponse.cookies, Seq.empty[ApiErrorModel], 200)
      }
    })
  }

  def getSaksFirstSummaryPage(headers: Map[String, String], accountId: String): Future[(SaksFirstSummaryPage, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("AccountService.getSaksFirstSummaryPage")({
      for {
        saksPageEnabled <- togglesHelper.saksFirstPageEnabled
        redemptionDisabled <- togglesHelper.disableRedemption
        beautyToggleEnabled <- togglesHelper.beautyEnabled
        response <- httpTransportService.getFromService[SaksFirstSummaryPage](userAccountServiceUrl, Some(s"accounts/$accountId/loyalty-program/summary"), headers).map {
          case Left(failureResponse) =>
            val summary = SaksFirstSummaryPage(false, false, false, MemberInfo.empty, None, None, None, accountHelper.buildSaksFirstSummaryPageMessages, accountHelper.buildSaksFirstSummaryPageLinks(accountId, headers), Some(accountHelper.getHeader(saksPageEnabled = saksPageEnabled)))
            (summary, failureResponse.cookies, failureResponse.errors, failureResponse.code)
          case Right(successfulResponse) =>
            val beautyEnabled: Boolean = beautyToggleEnabled && successfulResponse.body.enabled_beauty
            val summary = SaksFirstSummaryPage(successfulResponse.body.enabled, !redemptionDisabled, beautyEnabled, successfulResponse.body.member_info, successfulResponse.body.gift_card_history, successfulResponse.body.user_loyalty_info,
              successfulResponse.body.beauty, accountHelper.buildSaksFirstSummaryPageMessages, accountHelper.buildSaksFirstSummaryPageLinks(accountId, headers), Some(accountHelper.getHeader(saksPageEnabled = saksPageEnabled)))
            (summary, successfulResponse.cookies, Seq.empty[ApiErrorModel], 200)
        }
      } yield response
    })
  }

  def getBeautyBoxRequestForUser(headers: Map[String, String], accountId: String, emailAddress: String): Future[Either[Seq[ApiErrorModel], BeautyBoxRequest]] = {
    statsDClient.time("AccountService.getBeautyBoxesForUser")({
      for {
        response <- httpTransportService.getFromService[SaksFirstSummaryPage](userAccountServiceUrl, Some(s"accounts/$accountId/loyalty-program/summary"), headers).map {
          case Left(failureResponse) => Left(failureResponse.errors)
          case Right(successfulResponse) =>
            val recipients = Emails(List(emailAddress), List(), List())
            successfulResponse.body.beauty match {
              case Some(saksFirstBeauty) => saksFirstBeauty.boxes match {
                case Some(beautyBoxes) => Right(BeautyBoxRequest(beautyBoxes, recipients))
                case None              => Left(List(ApiErrorModel("User has no beauty boxes", "get_beauty_box_request_for_user")))
              }
              case None => Left(List(ApiErrorModel("User does not have SaksFirstBeauty enabled", "get_beauty_box_request_for_user")))
            }
        }
      } yield response
    })
  }

  def sendBeautyBoxEmail(headers: Map[String, String], accountId: String, beautyBoxEvent: BeautyBoxRequest): Future[(Option[String], Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("AccountService.sendBeautyBoxEmail") {
      httpTransportService.postToExternalService[BeautyBoxRequest, ApiResultModel](customerNotificationServiceUrl, Some(s"events"), beautyBoxEvent, headers).map {
        case Left(failureResponse)     => (None, failureResponse.cookies, failureResponse.errors, failureResponse.code)
        case Right(successfulResponse) => (Some(successfulResponse.body.results.toString()), successfulResponse.cookies, Seq.empty, 200)
      }
    }
  }

  def timingOutIn[T](awaitableFuture: => Future[T], atMost: FiniteDuration): Future[T] = {
    lazy val timeOutPromise = after(duration = atMost, using = actorSystem.scheduler)(Future.failed(new TimeoutException("More service timed out!")))
    Future firstCompletedOf Seq(timeOutPromise, awaitableFuture)
  }

  def redeem(headers: Map[String, String], accountId: String, redeemRequest: RedemptionRequest): Future[(Option[SaksFirstAccountSummary], Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("AccountService.redeemPoints") {
      httpTransportService.postToService[RedemptionRequest, SaksFirstAccountSummary](userAccountServiceUrl, Some(s"accounts/$accountId/loyalty-program/redeem"), redeemRequest, headers).map {
        case Left(failureResponse)     => (None, failureResponse.cookies, failureResponse.errors, failureResponse.code)
        case Right(successfulResponse) => (Some(successfulResponse.body), successfulResponse.cookies, Seq.empty, 200)
      }
    }
  }
}
