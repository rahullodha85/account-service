package controllers

import javax.inject.Inject

import helpers.{ AccountHelper, ControllerPayload, RequestHelper }
import messages.HBCMessagesApi
import models.website.{ ChangePasswordRequest, EmailPreferencesRequest }
import play.api.i18n.I18nSupport
import play.api.mvc.Controller
import services.AccountService
import validations.Validator
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future

class AccountSettingsController @Inject() (
    accountService:  AccountService,
    validator:       Validator,
    accountHelper:   AccountHelper,
    val messagesApi: HBCMessagesApi
) extends Controller with I18nSupport with ControllerPayload with RequestHelper {

  def getAccountSettings(account_id: String) = Authorized { (headers, request) =>
    implicit val req = request
    accountService.getAccountSettings(headers, account_id).map { e =>
      writeResponseGet(e._1, e._3).withCookies(e._2: _*)
    }
  }

  def updateAccountSettings(account_id: String) = AuthorizedWithNoBM(accountService, account_id, { (userName, headers, request) =>
    implicit val req = request
    validator.validate[EmailPreferencesRequest](request.body.asJson) match {
      case Right(v) =>
        accountService.updateEmailPreferences(v.toPreferencesRequestModel(headers, userName), headers).map { e =>
          writeResponseSuccess(e._1, Status(e._4), e._3).withCookies(e._2: _*)
        }
      case Left(errors) =>
        Future.successful(writeResponseError(errors, Status(400)))
    }
  })

  def changePasswordAction(account_id: String) = Authorized { (headers, request) =>
    implicit val req = request
    validator.validate[ChangePasswordRequest](request.body.asJson) match {
      case Right(changePasswordRequest) =>
        accountService.changePasswordAction(headers, changePasswordRequest).map { e =>
          writeResponse(e)
        }
      case Left(errors) =>
        Future.successful(writeResponseError(errors, Status(400)))
    }
  }
}

