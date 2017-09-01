package controllers

import javax.inject.Inject

import constants.Constants.LOYALTY_ID
import helpers.{ ControllerPayload, RequestHelper, TogglesHelper }
import models.ApiErrorModel
import models.servicemodel.{ BeautyBoxRequest, MemberInfo, SaksFirstAccountSummary }
import models.website._
import play.api.mvc._
import services.{ AccountService, PaymentMethodService, RewardsService }
import validations.Validator

import scala.concurrent.Future
import scala.util.Success

class RewardsController @Inject() (
    togglesHelper:        TogglesHelper,
    rewardsService:       RewardsService,
    accountService:       AccountService,
    validator:            Validator,
    paymentMethodService: PaymentMethodService
) extends Controller with ControllerPayload with RequestHelper {

  import play.api.libs.concurrent.Execution.Implicits._

  def linkSaksFirst(account_id: String) = AuthorizedWithNoBM(accountService, account_id, { (_, headers, request) =>
    implicit val req = request

    def createPaymentMethodRequest(linkSaksFirstRequest: LinkSaksFirstRequest) = CreatePaymentMethodRequest(false, "SAKS", linkSaksFirstRequest.name, linkSaksFirstRequest.saks_first_number, None, None, Option(linkSaksFirstRequest.zip))

    validator.validate[LinkSaksFirstRequest](request.body.asJson) match {
      case Right(linkSaksFirstRequest) =>
        rewardsService.linkSaksFirstAccount(headers, linkSaksFirstRequest, account_id) andThen {
          case Success(x) =>
            if (x._3.isEmpty) paymentMethodService.createPaymentMethod(headers, createPaymentMethodRequest(linkSaksFirstRequest), account_id)
        } map { e => writeResponse(e) }
      case Left(errors) =>
        Future.successful(writeResponseError(errors, Status(400)))
    }
  })

  def getSaksFirstInfo(account_id: String) = Action.async { implicit request =>
    def returnDisabledResponseWithNoAuthenticationCall(request: Request[AnyContent]): Future[Result] = {
      Unauthenticated { (headers, request) =>
        Future.successful(writeResponse(SaksFirstAccountSummary(false, MemberInfo.empty, None, None), Seq.empty, Seq.empty, 200)(request, SaksFirstAccountSummary.saksFirst)
          .discardingCookies(new DiscardingCookie(LOYALTY_ID)))
      }(request)
    }

    for {
      summaryEnabled <- togglesHelper.saksFirstSummaryEnabled

      response <- if (summaryEnabled) {
        AuthorizedWithNoBM(accountService, account_id, { (_, headers, request) =>
          implicit val req = request
          rewardsService.getSaksFirstAccountSummary(headers, account_id).map {
            response =>
              respondWithLoyaltyIdCookie(response)
          }
        })(request)
      } else {
        returnDisabledResponseWithNoAuthenticationCall(request)
      }
    } yield response
  }

  def getSaksFirstSummaryPage(account_id: String) = AuthorizedWithNoBM(accountService, account_id, { (_, headers, request) =>
    implicit val req = request
    rewardsService.getSaksFirstSummaryPage(headers, account_id).map {
      response => writeResponse(response)
    }
  })

  def sendBeautyBoxEmail(account_id: String) = AuthorizedWithNoBM(accountService, account_id, { (email, headers, request) =>
    implicit val req = request
    rewardsService.getBeautyBoxRequestForUser(headers, account_id, email).flatMap {
      maybeBeautyBoxRequest =>
        {
          maybeBeautyBoxRequest match {
            case Right(beautyBoxRequest) => rewardsService.sendBeautyBoxEmail(headers, account_id, beautyBoxRequest).map {
              response => writeResponse(response)
            }
            case Left(errors) => Future.successful(writeResponseError(errors, Status(400)))
          }
        }
    }
  })

  private def respondWithLoyaltyIdCookie(response: (Option[SaksFirstAccountSummary], Seq[Cookie], Seq[ApiErrorModel], Int))(implicit request: Request[_]): Result = {
    response._1 match {
      case Some(saksFirstSummary) =>
        saksFirstSummary.member_info.loyalty_id match {
          case loyaltyId if loyaltyId.isEmpty =>
            writeResponse(response).discardingCookies(new DiscardingCookie(LOYALTY_ID))
          case loyaltyId =>
            writeResponse(response).withCookies(new Cookie(LOYALTY_ID, loyaltyId, httpOnly = false))
        }
      case None =>
        writeResponse(response).discardingCookies(new DiscardingCookie(LOYALTY_ID))
    }
  }

  def redeem(account_id: String) = AuthorizedWithNoBM(accountService, account_id, { (_, headers, request) =>
    implicit val req = request
    for {
      redemptionDisabled <- togglesHelper.disableRedemption
      response <- if (!redemptionDisabled) {
        validator.validate[RedemptionRequest](request.body.asJson) match {
          case Right(redemptionRequest) =>
            rewardsService.redeem(headers, account_id, redemptionRequest).map {
              response => writeResponse(response)
            }
          case Left(errors) =>
            Future.successful(writeResponseError(errors, Status(400)))
        }
      } else {
        Future.successful(writeResponseError(Seq(ApiErrorModel("Redemption disabled", "redemption.disabled")), Status(403)))
      }
    } yield response
  })
}
