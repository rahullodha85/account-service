package controllers.old

import javax.inject.Inject

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

  def logout = Action.async { implicit request =>
    accountService.logout(request.headers.toSimpleMap).map { e =>
      writeResponse(e)
    }
  }
}
