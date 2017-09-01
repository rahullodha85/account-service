package controllers

import javax.inject.Inject

import helpers._
import models.website._
import play.api.mvc._
import services.{ AccountService, UserAccountService }
import validations.Validator

import scala.concurrent.Future

class AccountsController @Inject() (
    accountService:     AccountService,
    userAccountService: UserAccountService,
    validator:          Validator,
    accountHelper:      AccountHelper,
    togglesHelper:      TogglesHelper
) extends Controller with ControllerPayload with RequestHelper {

  import play.api.libs.concurrent.Execution.Implicits._

  def createAccountPage = Action.async { implicit request =>
    var registerOptions = EmailSubscriptionsModelBuilder.buildRegisterOptions(ConfigHelper.banner)
    togglesHelper.giltEmailSubscription.flatMap { enabled =>
      if (enabled) {
        registerOptions = EmailSubscriptionsModelBuilder.buildGiltRegisterOptions(ConfigHelper.banner)
      }
      Future.successful(writeResponseGet(
        RegisterWebsiteModel(
          messages = accountHelper.buildRegisterMessages,
          links = accountHelper.buildRegisterLinks(request.headers.toSimpleMap),
          canadian_customer = accountHelper.buildCanadianCustomerModel(),
          email_subscriptions = registerOptions
        )
      ))
    }
  }

  def getAccountSummary(account_id: String) = Authorized { (headers, request) =>
    implicit val req = request
    implicit val headers_ = headers
    accountService.getAccountSummary(account_id).map { e =>
      writeResponseGet(e._1, e._3).withCookies(e._2: _*)
    }
  }

  def getAccount(account_id: String) = Authorized { (headers, request) =>
    implicit val req = request
    accountService.getAccount(headers).map { e =>
      writeResponseSuccess(e._1, Status(e._4), e._3).withCookies(e._2: _*)
    }
  }

  def createAccount = Unauthenticated { (headers, request) =>
    implicit val req = request
    validator.validate[CreateAccountRequest](request.body.asJson) match {
      case Right(createAccountRequest) =>
        userAccountService.createAccount(headers, createAccountRequest, headers.getOrElse("X-Real-IP", "")).map { response =>
          writeResponse(response)
        }
      case Left(errors) =>
        Future.successful(writeResponseError(errors, Status(400)))
    }
  }

  def updateAccount(account_id: String) = Authorized { (headers, request) =>
    implicit val req = request
    validator.validate[UpdateAccountRequest](request.body.asJson) match {
      case Right(updateAccountRequest) =>
        userAccountService.updateProfile(headers, updateAccountRequest).map { e =>
          writeResponse(e)
        }
      case Left(errors) =>
        Future.successful(writeResponseError(errors, Status(400)))
    }
  }
}
