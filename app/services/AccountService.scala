package services

import javax.inject.Inject

import constants.Banners
import constants.Constants._
import models.Constants._
import helpers.{AccountHelper, ConfigHelper, TogglesHelper}
import models.servicemodel.{PageInfo, _}
import models.website.{ResetPasswordRequest, _}
import models.{ApiErrorModel, FailureResponse, SuccessfulResponse}
import monitoring.StatsDClientLike
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsObject, Json, Writes}
import play.api.mvc.Cookie

import scala.concurrent.{Future, TimeoutException}

class AccountService @Inject()(
    httpTransportService: HttpTransportService,
    rewardsService:       RewardsService,
    localizationService:  LocalizationService,
    statsDClient:         StatsDClientLike,
    togglesHelper:        TogglesHelper,
    accountHelper:        AccountHelper,
    configHelper:         ConfigHelper
) {

  val userAuthenticationServiceUrl: String = configHelper.getStringProp("data-service.user-authentication")
  val orderServiceUrl: String = configHelper.getStringProp("data-service.order")
  val userAccountServiceUrl: String = configHelper.getStringProp("data-service.user-account")
  val emailMarketingServiceUrl: String = configHelper.getStringProp("data-service.email-marketing")

  def getAccount(headers: Map[String, String]): Future[(Option[SignedInWebsiteModel], Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("AccountService.getAccount"){
       getAccountFromSvc(headers).map {
        case Left(failureResponse) => (None, failureResponse.cookies, failureResponse.errors, failureResponse.code)
        case Right(s) => (Some(SignedInWebsiteModel.fromUserAccount(s.body, accountHelper.accountTitle)), s.cookies, Seq.empty, 200)
      }
    }
  }

  private def combineCookies(headers: Map[String, String], cookies: Seq[Cookie]) = {
    val sessionCookie = cookies.find { x => x.name.equals("JSESSIONID") }
    sessionCookie match {
      case Some(cookie) => headers.get("Cookie") match {
        case Some(value) =>
          val cookieHeaderValue = value + s"$JSESSIONID=${cookie.value}"
          //Remove existing Cookie header and replace it with new value
          val newHeader = headers - "Cookie"
          newHeader + ("Cookie" -> cookieHeaderValue)
        case None => headers
      }
      case None => headers
    }
  }

  private def buildSuccessfulOrdersWebsiteModel(serviceData: SuccessfulResponse[Orders], links: Option[Map[String, String]], messages: Option[Map[String, String]], countries: Map[String, Country]): (OrdersWebsiteModel, Seq[Cookie], Seq[ApiErrorModel], Int) = {
    val ordersResponse: Orders = serviceData.body
    val servicePageInfo = ordersResponse.page_info
    val currentItems = if (servicePageInfo.current_items > ACCOUNT_SUMMARY_NUMBER_OF_ORDERS) ACCOUNT_SUMMARY_NUMBER_OF_ORDERS else servicePageInfo.current_items
    val pageInfo = PageInfo(servicePageInfo.current_page, ACCOUNT_SUMMARY_NUMBER_OF_ORDERS, servicePageInfo.total_items, currentItems)
    (OrdersWebsiteModel(enabled = true, ordersResponse.orders.take(ACCOUNT_SUMMARY_NUMBER_OF_ORDERS), pageInfo, links, messages, countries), serviceData.cookies, Seq.empty[ApiErrorModel], 200)
  }

  def getAccountSummary(accountId: String)(implicit headers: Map[String, String]): Future[(AccountSummaryResponseModel, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    def orders(userAccountResponse: Either[FailureResponse, SuccessfulResponse[UserAccount]], headers: Map[String, String]) = {
      val updatedHeaders = userAccountResponse match {
        case Right(accountResponse) => combineCookies(headers, accountResponse.cookies)
        case Left(failureResponse) => combineCookies(headers, failureResponse.cookies)
      }
      statsDClient.time("AccountService.getAccountSummary.getOrderHistory") {
        httpTransportService.getFromService[Orders](orderServiceUrl, Some("orders"), updatedHeaders, Map(PAGE_NUM_PARAM -> "1"))
      }
    }

    statsDClient.time("AccountService.getAccountSummary")({
      val userAccount = statsDClient.time("AccountService.getAccountSummary.getAccount"){ getAccountFromSvc(headers) }
      val countriesResponse = localizationService.cachedCountries("countries")
      for {
        userAccountResponse <- userAccount
        userAccountRewardsResponse <- configHelper.banner match {
          case Banners.Saks => rewardsResponseSaks(userAccountResponse, accountId)
          case Banners.Off5th => rewardsResponseOFF5th(userAccountResponse, accountId)
          case Banners.LordAndTaylor => rewardsResponseLT(userAccountResponse, accountId)
        }
        ordersResponse <- orders(userAccountResponse, headers)
        countries <- countriesResponse
        addFavoritesTab <- togglesHelper.getFavoritesToggleState
        addSaksFirstPage <- togglesHelper.saksFirstPageEnabled
      } yield {
        val order = ordersResponse match {
          case Left(failureResponse) => (OrdersWebsiteModel(enabled = false, Seq.empty, PageInfo(0, 0, 0, 0), accountHelper.buildViewMoreOrderLinks(accountId), accountHelper.buildOrderMessages, countries), failureResponse.cookies, failureResponse.errors, 200)
          case Right(s) => buildSuccessfulOrdersWebsiteModel(s, accountHelper.buildViewMoreOrderLinks(accountId), accountHelper.buildOrderMessages, countries)
        }
        val userAccount = userAccountResponse match {
          case Left(e) => (false, UserAccount.emptyModel)
          case Right(s) => (true, s.body)
        }
        val userAccountWebsiteModel = UserAccountWebsiteModel(userAccount._1, userAccount._2, accountHelper.buildUserAccountLinks(accountId), accountHelper.buildUserAccountMessages)
        val apiErrors: Seq[ApiErrorModel] = userAccountRewardsResponse._2 ++ order._3
        val cookies: Seq[Cookie] = order._2
        (AccountSummaryResponseModel(userAccountWebsiteModel, userAccountRewardsResponse._1, order._1, accountHelper.getHeader(addFavoritesTab, addSaksFirstPage), accountHelper.buildAccountServiceDownMessage), cookies, apiErrors, 200)
      }
    })
  }

  def getAccountFromSvc(headers: Map[String, String]): Future[Either[FailureResponse, SuccessfulResponse[UserAccount]]] = {
        httpTransportService.getFromService[UserAccount](userAccountServiceUrl, Some("get_account"), headers)
  }


  def rewardsResponseSaks(userRes: Either[FailureResponse, SuccessfulResponse[UserAccount]], accountId: String)(implicit headers: Map[String, String]): Future[(RewardsWebsiteModel, Seq[ApiErrorModel])] = {
    val saksfirst = "saksfirst"
    val saksfirstLinks = accountHelper.buildSaksFirstLinks(headers, accountId)
    val saksFirstMessages = accountHelper.buildSaksFirstMessages
    userRes match {
      case Left((failureResponse)) =>
        Future.successful((RewardsWebsiteModel(enabled = false, saksfirst, rewards_threshold = None, more_rewards_data = None, saksfirst_rewards_data = Option(SaksFirstModel(false, false, MemberInfo.empty)), saksfirstLinks, saksFirstMessages), Seq.empty))
      case Right(successfulResponse) => {
        rewardsService.getSaksFirstAccountSummary(headers, accountId).flatMap {
          case (maybeSummary, _, _, _) =>
            val model = maybeSummary match {
              case (Some(saksFirstAccountSummary)) => SaksFirstModel(saksFirstAccountSummary.member_info.linked, saksFirstAccountSummary.enabled, saksFirstAccountSummary.member_info)
              case _ =>  SaksFirstModel(false, false, MemberInfo.empty)
            }
            Future.successful((RewardsWebsiteModel(enabled = true, saksfirst, Some(SAKS_FIRST_REWARDS_THRESHOLD), more_rewards_data = None, saksfirst_rewards_data = Option(model), saksfirstLinks, saksFirstMessages), Seq.empty[ApiErrorModel]))
        }
      }
    }
  }

  def rewardsResponseOFF5th(userRes: Either[FailureResponse, SuccessfulResponse[UserAccount]], accountId: String)(implicit headers: Map[String, String]): Future[(RewardsWebsiteModel, Seq[ApiErrorModel])] = {
    val more = "more"
    val banner = configHelper.banner
    userRes match {
      case Left((failureResponse))  => Future.successful((RewardsWebsiteModel(enabled = false, more, rewards_threshold = None, more_rewards_data = None, saksfirst_rewards_data = None, accountHelper.buildMoreLinks, accountHelper.buildMoreMessages), Seq.empty))
      case Right(successfulResponse) =>
            statsDClient.time("RewardsService.getAccountSummary.getMoreNumber")({
              rewardsService.retrieveMoreAccount(successfulResponse.body, headers).map {
                case Left(e1) => (RewardsWebsiteModel(enabled = false, more, rewards_threshold = None, more_rewards_data = None, saksfirst_rewards_data = None, accountHelper.buildMoreLinks, accountHelper.buildMoreMessages), e1.errors)
                case Right(v) => (RewardsWebsiteModel(enabled = true,  more, rewards_threshold = None, more_rewards_data = Option(MoreModel(v.body.more_number)), saksfirst_rewards_data = None, accountHelper.buildMoreLinks, accountHelper.buildMoreMessages), Seq.empty[ApiErrorModel])
              } recoverWith {
                case e: TimeoutException =>
                  Logger.warn("More service timed out, returning empty response")
                  Future.successful((RewardsWebsiteModel(enabled = false, more, rewards_threshold = None,  more_rewards_data = None, saksfirst_rewards_data = None, accountHelper.buildMoreLinks, accountHelper.buildMoreMessages), Seq.empty))
              }
            })
    }
  }

  def rewardsResponseLT(userRes: Either[FailureResponse, SuccessfulResponse[UserAccount]], accountId: String)(implicit headers: Map[String, String]): Future[(RewardsWebsiteModel, Seq[ApiErrorModel])] = {
    val stubbedLTRewardModel = Future.successful(RewardsWebsiteModel(enabled = false, "saksfirst",
      Some(SAKS_FIRST_REWARDS_THRESHOLD), more_rewards_data = None, saksfirst_rewards_data = None,
      accountHelper.buildSaksFirstLinks(headers, accountId), accountHelper.buildSaksFirstMessages), Seq.empty)
    stubbedLTRewardModel
  }

  def getAccountSettings(headers: Map[String, String], accountId: String): Future[(AccountSettingsResponseModel, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("AccountService.getAccountSettings") {
      for {
        addFavoritesTab <- togglesHelper.getFavoritesToggleState
        addSaksFirstTab <- togglesHelper.saksFirstPageEnabled
        accountResponseEither <- getAccountFromSvc(headers)
        emailResponse <- accountResponseEither match {
          case Right(accountResponse) =>
            getEmailPrefsWithEms(accountResponse, headers, accountId)
          case Left(failureResponse) =>
            Future.successful((EmailPreferencesResponseModel(enabled = false, getEmailPreferencesOptions(Seq.empty[String]), accountHelper.buildEmailPreferencesLinks(accountId), accountHelper.buildEmailPreferencesMessages), failureResponse.cookies, failureResponse.errors))
        }
        userAccountResponseModel <- accountResponseEither match {
          case Right(accountResponse) =>
            Future.successful(UserAccountWebsiteModel(enabled = true, accountResponse.body, accountHelper.buildUserAccountLinks(accountId), accountHelper.buildUserAccountMessages))
          case Left(failedResponse) =>
            Future.successful(UserAccountWebsiteModel(enabled = false, UserAccount.emptyModel, accountHelper.buildUserAccountLinks(accountId), accountHelper.buildUserAccountMessages))
        }
      } yield {
        val passwordSettingModel = PasswordSettingModel(enabled = true, accountHelper.buildPwdSettingsLinks(accountId), accountHelper.buildPwdSettingsMessages)
        val accountSettingsModel = AccountSettingsResponseModel(
          userAccountResponseModel,
          passwordSettingModel,
          emailResponse._1,
          accountHelper.getHeader(addFavoritesTab, addSaksFirstTab),
          Some(accountHelper.buildAccountServiceDownMessage)
        )
        (accountSettingsModel, emailResponse._2, emailResponse._3, 200)
      }
    }
  }

  private def getEmailPrefsWithEms(accountResponse: SuccessfulResponse[UserAccount], headers: Map[String, String], accountId: String): Future[(EmailPreferencesResponseModel, Seq[Cookie], Seq[ApiErrorModel])] = {
    statsDClient.time("AccountService.getEmailPreferences") {
          httpTransportService.getFromService[Seq[EmailPreferencesModel]](emailMarketingServiceUrl, Some(s"profile/${accountResponse.body.user_id}"), headers) map {
        case Left(failureResponse) =>
          (EmailPreferencesResponseModel(
            enabled = false,
            getEmailPreferencesOptions(Seq.empty[String]),
            accountHelper.buildEmailPreferencesLinks(accountId),
            accountHelper.buildEmailPreferencesMessages
          ), failureResponse.cookies, failureResponse.errors)
        case Right(emailPreferences) =>
          (EmailPreferencesResponseModel(
            enabled = true,
            EmailSubscriptionsModelBuilder.buildUpdatePreferencesOptions(configHelper.banner, getUserEmailPreferences(emailPreferences)),
            accountHelper.buildEmailPreferencesLinks(accountId),
            accountHelper.buildEmailPreferencesMessages
          ), Seq.empty, Seq.empty[ApiErrorModel])
      }
    }
  }

  def getEmailPreferencesOptions(preferences: Seq[String]): EmailSubscriptionsModel = {
    EmailSubscriptionsModelBuilder.buildUpdatePreferencesOptions(configHelper.banner, preferences)
  }

  private def getUserEmailPreferences(response: SuccessfulResponse[Seq[EmailPreferencesModel]]): Seq[String] = {
    response.body.headOption.getOrElse(EmailPreferencesModel()).toResponsePayload
  }


  def updateEmailPreferences(preferencesRequestModel: PreferencesRequestModel, headers: Map[String, String]): Future[(PreferencesResponseModel, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("AccountService.updateEmailPreferences") {
      val httpModel = Json.obj("item" -> preferencesRequestModel)
      httpTransportService.putToService[JsObject, Seq[EmailPreferencesModel]](emailMarketingServiceUrl, Some(s"profile/${preferencesRequestModel.email_address}/preferences"), httpModel, headers) map {
        case Left(failureResponse) => (PreferencesResponseModel(Seq()), failureResponse.cookies, Seq.empty, 503)
        case Right(emailPreferences) => (PreferencesResponseModel(getUserEmailPreferences(emailPreferences)), Seq.empty, Seq.empty, 200)
      }
    }
  }


  def signInAction[T <: SignInRequest](headers: Map[String, String], signInRequest: T)(implicit writes: Writes[T]): Future[(Option[SignedInWebsiteModel], Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("AccountService.signInAction")({
      httpTransportService.postToService[T, UserSignInAuthentication](userAuthenticationServiceUrl, Some("login"), signInRequest, headers).map {
        case Left(failureResponse) => (None, failureResponse.cookies, failureResponse.errors, failureResponse.code)
        case Right(successfulResponse) => (Some(SignedInWebsiteModel.fromUserAccount(successfulResponse.body, accountHelper.accountTitle)), successfulResponse.cookies, Seq.empty[ApiErrorModel], 200)
      }
    })
  }

  def forgotPasswordAction(headers: Map[String, String], requestJson: ForgotPasswordRequest): Future[(SuccessResponse, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("AccountService.forgotPasswordAction")({
      httpTransportService.postToService[ForgotPasswordRequest, SuccessResponse](userAuthenticationServiceUrl, Some("forgot_password"), requestJson, headers) map {
        case Left(failureResponse) => (SuccessResponse(false), failureResponse.cookies, failureResponse.errors, failureResponse.code)
        case Right(successfulResponse) => (SuccessResponse(successfulResponse.body.success), successfulResponse.cookies, Seq.empty[ApiErrorModel], 200)
      }
    })
  }

  def validateResetPassword(headers: Map[String, String], loc: String): Future[(ResetPasswordLabel, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("AccountService.validateResetPassword")({
          httpTransportService.getFromService[SuccessResponse](userAuthenticationServiceUrl, Some("validate_reset_password"), headers,Map(LOC_LABEL -> loc)) map {
          case Left(failureResponse) => (ResetPasswordLabel(accountHelper.buildPasswordResetMessages(), accountHelper.buildPasswordResetLinks(loc)), failureResponse.cookies, failureResponse.errors, failureResponse.code)
          case Right(successfulResponse) => (ResetPasswordLabel(accountHelper.buildPasswordResetMessages(), accountHelper.buildPasswordResetLinks(loc)), successfulResponse.cookies, Seq.empty[ApiErrorModel], 200)
        }
    })
  }

  def resetPasswordAction(headers: Map[String, String], resetPasswordRequest: ResetPasswordRequest, Loc: String): Future[(ResetPasswordWebsiteModel, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("AccountService.resetPasswordAction")({
      httpTransportService.postToService[ResetPasswordRequest, UserAuthResetPassword](userAuthenticationServiceUrl, Some("reset_password"), resetPasswordRequest, headers, Map(LOC_LABEL -> Loc)).map {
        case Left(failureResponse) => (ResetPasswordWebsiteModel(None, "", "", "", false, ResetPasswordTitleObject("", "")), failureResponse.cookies, failureResponse.errors, failureResponse.code)
        case Right(successfulResponse) => (ResetPasswordWebsiteModel(successfulResponse.body.id, successfulResponse.body.email, successfulResponse.body.first_name, successfulResponse.body.last_name, successfulResponse.body.is_account_cleared, accountHelper.buildResetPasswordTitle), successfulResponse.cookies, Seq.empty[ApiErrorModel], 200)
      }
    })
  }

  def changePasswordAction(headers: Map[String, String], changePasswordRequest: ChangePasswordRequest): Future[(SuccessResponse, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("AccountService.changePasswordAction")({
      httpTransportService.postToService[ChangePasswordRequest, UserAuthChangePassword](userAuthenticationServiceUrl, Some("change_password"), changePasswordRequest, headers).map {
        case Left(failureResponse) => (SuccessResponse(false), failureResponse.cookies, failureResponse.errors, failureResponse.code)
        case Right(successfulResponse) => (SuccessResponse(true), successfulResponse.cookies, Seq.empty[ApiErrorModel], 200)
      }
    })
  }

  def logout(headers: Map[String, String]): Future[(Option[SignOutWebsiteModel], Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("AccountService.logout")({
          httpTransportService.getFromService[SuccessResponse](userAuthenticationServiceUrl, Some("user/logout"), headers) map {
          case Left(failureResponse) => (None, failureResponse.cookies, failureResponse.errors, failureResponse.code)
          case Right(successfulResponse) => (Some(SignOutWebsiteModel(accountHelper.buildHomePageLink)), successfulResponse.cookies, Seq.empty[ApiErrorModel], 200)
        }
    })
  }
}
