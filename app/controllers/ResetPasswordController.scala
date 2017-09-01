package controllers

import javax.inject.Inject

import constants.Constants._
import helpers.{ AccountHelper, ControllerPayload, RequestHelper }
import messages.HBCMessagesApi
import models.website.{ ForgotPasswordLabel, ForgotPasswordRequest, LockedAccountLabel, ResetPasswordRequest }
import play.api.i18n.I18nSupport
import play.api.mvc.{ Action, Cookie }
import play.mvc.Controller
import services.AccountService
import validations.Validator
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

/**
 * Created by Automation on 2/3/17.
 */
class ResetPasswordController @Inject() (
    accountService:  AccountService,
    validator:       Validator,
    accountHelper:   AccountHelper,
    val messagesApi: HBCMessagesApi
) extends Controller with I18nSupport with ControllerPayload with RequestHelper {

  def forgotPasswordPage = Action { implicit request =>
    writeResponseGet(ForgotPasswordLabel(accountHelper.buildForgotPasswordLinks, accountHelper.buildForgotPasswordMessages(), LockedAccountLabel(accountHelper.buildLockedAccountLabelLinks, accountHelper.buildAccountLockedMessages())))
  }

  def forgotPasswordAction = Unauthenticated { (headers, request) =>
    implicit val req = request
    validator.validate[ForgotPasswordRequest](request.body.asJson) match {
      case Right(passwordRequest) =>
        accountService.forgotPasswordAction(headers, passwordRequest).map { e =>
          writeResponseSuccess(e._1, Status(e._4), e._3).withCookies(e._2: _*)
        }
      case Left(errors) =>
        Future.successful(writeResponseError(errors, Status(400)))
    }
  }

  def resetPasswordPage(Loc: String) = Action.async { implicit request =>
    accountService.validateResetPassword(request.headers.toSimpleMap, Loc).map { e =>
      writeResponse(e)
    }
  }

  def resetPasswordAction(Loc: String) = Action.async { implicit request =>
    validator.validate[ResetPasswordRequest](request.body.asJson) match {
      case Right(resetPasswordRequest) =>
        accountService.resetPasswordAction(request.headers.toSimpleMap, resetPasswordRequest, Loc).map { response =>
          writeResponse(response)
        }
      case Left(errors) =>
        Future.successful(writeResponseError(errors, Status(400)))
    }
  }
}
