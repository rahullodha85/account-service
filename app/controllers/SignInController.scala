package controllers

import javax.inject.Inject

import constants.Constants.LOYALTY_ID
import helpers.{ AccountHelper, ControllerPayload, RequestHelper }
import models.website._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import services.AccountService
import validations.Validator

import scala.concurrent.Future

class SignInController @Inject() (
    accountService: AccountService,
    validator:      Validator,
    accountHelper:  AccountHelper
) extends Controller with ControllerPayload with RequestHelper {

  def signInAction = Unauthenticated { (headers, request) =>
    implicit val req = request
    validator.validate[ClientSignInRequest](request.body.asJson) match {
      case Right(signIn) =>
        accountService.signInAction[ClientSignInRequest](headers, signIn).map { response =>
          writeResponse(response)
        }
      case Left(errors) =>
        Future.successful(writeResponseError(errors, Status(400)))
    }
  }

  def signInCsrAction = Unauthenticated { (headers, request) =>
    implicit val req = request
    validator.validate[CSRSignInRequest](request.body.asJson) match {
      case Right(signInRequest) =>
        accountService.signInAction[CSRSignInRequest](headers, signInRequest).map { response =>
          writeResponse(response)
        }
      case Left(errors) =>
        Future.successful(writeResponseError(errors, Status(400)))
    }
  }

  def signInPage() = Action { implicit request =>
    writeResponseGet(SignInObject(
      SignInUserLabel(accountHelper.buildSignInLinks, accountHelper.buildSignInMessages()),
      CreateAccountLabel(accountHelper.buildCreateAccountMessages),
      OrderStatusLabel(accountHelper.buildCheckOrderStatusMessages()),
      LockedAccountLabel(accountHelper.buildLockedAccountLabelLinks, accountHelper.buildAccountLockedMessages())
    ))
  }

  def signInCsrPage() = Action { implicit request =>
    writeResponseGet(CsrUserSignInObject(
      SignInUserLabel(accountHelper.buildSignInCsrLinks, accountHelper.buildSignInCsrMessages()),
      OrderStatusLabel(accountHelper.buildCheckOrderStatusCsrMessages()),
      LockedAccountLabel(accountHelper.buildLockedAccountLabelLinks, accountHelper.buildAccountLockedMessages())
    ))
  }

  def signOut(account_id: String) = Action.async { implicit request =>
    accountService.logout(request.headers.toSimpleMap).map { e =>
      writeResponse(e).discardingCookies(new DiscardingCookie(LOYALTY_ID))
    }
  }
}
