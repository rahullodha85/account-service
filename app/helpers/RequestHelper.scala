package helpers

import constants.Constants.JSESSIONID
import models.{ ApiErrorModel, UnauthorizedException }
import play.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import services.AccountService

import scala.concurrent.Future

trait RequestHelper extends ControllerPayload {
  val headersSet = Set("Host", "User-Agent", "Cookie", "X-Forwarded-For", "X-Forwarded-Host", "X-Forwarded-Server", "X-Real-IP", "correlation-id")
  val headersSetNoCookie = Set("Host", "User-Agent", "X-Forwarded-For", "X-Forwarded-Host", "X-Forwarded-Server", "X-Real-IP", "correlation-id")

  def Unauthenticated(f: (Map[String, String], Request[AnyContent]) => Future[Result]): Action[AnyContent] = {
    Action.async { implicit request =>
      f(getRequestHeaders(request), request)
    }
  }

  def Authorized(f: (Map[String, String], Request[AnyContent]) => Future[Result]) = {
    Action.async { implicit request =>
      getJsessionId match {
        case Some(jsid) =>
          f(getRequestHeaders(request), request)
        case None => Future.successful(writeResponseError(Seq(ApiErrorModel("Unable to find JSESSIONID!", "JSessionIDNotFound")), Status(401)))
      }
    }
  }

  def AuthorizedWithNoBM(accountService: AccountService, accountId: String, f: (String, Map[String, String], Request[AnyContent]) => Future[Result]) = {
    Action.async { implicit request =>
      getJsessionId match {
        case Some(_) =>
          callBMToVerifyUserIsLoggedIn(accountService, request).flatMap {
            case (maybeSignedInWebsiteModel, cookies: Seq[Cookie], _, _) =>
              maybeSignedInWebsiteModel match {
                case Some(signedInWebsiteModel) =>
                  signedInWebsiteModel.id match {
                    case Some(accountIdAssociatedWithJSessionId) =>
                      if (accountIdAssociatedWithJSessionId != accountId) {
                        Logger.error(s"User $accountIdAssociatedWithJSessionId is attempting to access resource of $accountId but is not authorized to do so.")
                        throw UnauthorizedException("User does not have access to this resource", cookies)
                      }
                      f(signedInWebsiteModel.user_id, getRequestHeaders(request), request) map { response =>
                        response.withCookies(cookies: _*)
                      }
                    case None => throw UnauthorizedException("User is not logged in", cookies)
                  }
                case None => throw UnauthorizedException("User is not logged in", cookies)
              }
          }
        case None => Future.successful(writeResponseError(Seq(ApiErrorModel("Unable to find JSESSIONID!", "JSessionIDNotFound")), Status(401)))
      }
    }
  }

  private def callBMToVerifyUserIsLoggedIn(accountService: AccountService, request: Request[AnyContent]) = {
    accountService.getAccount(getRequestHeaders(request))
  }

  private def getRequestHeaders(request: Request[AnyContent]): Map[String, String] = {
    request.headers.toSimpleMap
  }
}

object RequestHelper extends RequestHelper
