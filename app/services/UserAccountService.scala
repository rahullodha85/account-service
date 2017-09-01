package services

import javax.inject.Inject

import constants.{ Banners, Constants }
import helpers.{ AccountHelper, ConfigHelper, TogglesHelper }
import models.servicemodel._
import models.website._
import models.{ ApiErrorModel, FailureResponse, SuccessfulResponse }
import monitoring.StatsDClientLike
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.Cookie

import scala.concurrent.Future

class UserAccountService @Inject() (
    httpTransportService:  HttpTransportService,
    rewardsService:        RewardsService,
    statsDClient:          StatsDClientLike,
    togglesHelper:         TogglesHelper,
    accountHelper:         AccountHelper,
    configHelper:          ConfigHelper,
    emailMarketingService: EmailMarketingService
) {

  val userAccountServiceUrl: String = configHelper.getStringProp("data-service.user-account")

  private def createBaseAccount(headers: Map[String, String], createAccountRequest: CreateAccountRequest): Future[(CreateAccountResponseWebsiteModel, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("UserAccountService.createAccount.createUserAccount")({
      httpTransportService.postToService[CreateAccountRequest, UserAccount](userAccountServiceUrl, Some("create_account"), createAccountRequest, headers).map {
        case Left(failureResponse)     => (CreateAccountResponseWebsiteModel(None, "", "", "", Option(""), "", AccountTitleObject("", "")), failureResponse.cookies, failureResponse.errors, failureResponse.code)
        case Right(successfulResponse) => (CreateAccountResponseWebsiteModel.fromUserAccount(successfulResponse.body, accountHelper.buildAccountTitle), successfulResponse.cookies, Seq.empty[ApiErrorModel], 200)
      }
    })
  }

  private def waitForMoreNumberAccount(headers: Map[String, String] = Map.empty, accountCreationResponse: Future[(CreateAccountResponseWebsiteModel, Seq[Cookie], Seq[ApiErrorModel], Int)], moreResponse: Future[Either[FailureResponse, SuccessfulResponse[More]]]) = {
    Logger.debug(s"${Constants.TOGGLE_WAIT_FOR_MORE_NUMBER} toggle is on: Waiting for more account service unless the call fails.")
    statsDClient.time("UserAccountService.createAccount.waitForMoreNumberAccount")({
      moreResponse.flatMap(_ => accountCreationResponse) recoverWith {
        case e: Throwable =>
          Logger.error(s"More account creation failed with error ${e.getMessage}", e)
          accountCreationResponse
      }
    })
  }

  private def responseWithMore(headers: Map[String, String], createAccountRequest: CreateAccountRequest, accountCreationResponse: Future[(CreateAccountResponseWebsiteModel, Seq[Cookie], Seq[ApiErrorModel], Int)]) = {
    val waitForMoreAccountToggleState = togglesHelper.getWaitForMoreToggleState
    val moreResponse = rewardsService.viewOrRegisterMoreAccount(createAccountRequest, headers)
    for {
      toggleState <- waitForMoreAccountToggleState
      response <- if (toggleState) waitForMoreNumberAccount(headers, accountCreationResponse, moreResponse) else accountCreationResponse
    } yield response
  }

  private def responseWithoutMore(accountCreationResponse: Future[(CreateAccountResponseWebsiteModel, Seq[Cookie], Seq[ApiErrorModel], Int)]) = {
    for {
      response <- accountCreationResponse
    } yield response
  }

  def createAccount(headers: Map[String, String], createAccountRequest: CreateAccountRequest, ipAddress: String): Future[(CreateAccountResponseWebsiteModel, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    val isShouldCallMore = configHelper.banner match {
      case Banners.Off5th => true;
      case _              => false
    }

    val emailPreferences = accountHelper.DefaultPreferences.defaultPreferences(createAccountRequest)

    statsDClient.time("UserAccountService.createAccount")({

      val accountCreationResponse = createBaseAccount(headers, createAccountRequest)

      emailMarketingService.createEmailPreferences(headers, createAccountRequest, ipAddress, emailPreferences)

      isShouldCallMore match {
        case true => responseWithMore(headers, createAccountRequest, accountCreationResponse)
        case _    => responseWithoutMore(accountCreationResponse)
      }
    })
  }

  def updateProfile(headers: Map[String, String], updateAccountRequest: UpdateAccountRequest): Future[(UpdateProfileResponseWebsiteModel, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("UserAccountService.updateProfile")({
      httpTransportService.getFromService[UserAccount](userAccountServiceUrl, Some("get_account"), headers) flatMap {
        case Right(oldAccountResponse) =>
          val updateAccountResponse = httpTransportService.postToService[UpdateAccountRequest, UserAccount](userAccountServiceUrl, Some("update_account"), updateAccountRequest, headers)
          val userId: String = oldAccountResponse.body.user_id

          val httpModel = Json.obj("item" -> updateAccountRequest)
          val updateEmailProfileResponse = emailMarketingService.updateEmailProfile(headers, userId, httpModel)
          for {
            updateUserProfile <- updateAccountResponse
            updateEmailProfile <- updateEmailProfileResponse
          } yield {
            val emailErrors = updateEmailProfile.left.getOrElse(FailureResponse(Seq.empty[ApiErrorModel], 200)).errors
            updateUserProfile match {
              case Left(failure)  => (UpdateProfileResponseWebsiteModel(UserAccount.emptyModel), failure.cookies, failure.errors ++ emailErrors, failure.code)
              case Right(success) => (UpdateProfileResponseWebsiteModel(success.body), success.cookies, emailErrors, 200)
            }
          }
        case Left(failure) =>
          Future.successful(UpdateProfileResponseWebsiteModel(UserAccount.emptyModel), failure.cookies, failure.errors, failure.code)
      }
    })
  }

}
