package helpers

import javax.inject.Inject

import constants.Banners
import constants.Constants._
import messages.HBCMessagesApi
import models.ApiErrorModel
import models.servicemodel._
import models.website._
import org.joda.time.DateTime
import play.Logger
import play.api.i18n.{ I18nSupport, Messages }

class AccountHelper @Inject() (val messagesApi: HBCMessagesApi, configHelper: ConfigHelper) extends I18nSupport {

  def accountTitle = AccountTitleObject(Messages("header_account_title"), Messages("blank_name_account_title"))

  def getMonths = Month(Seq(
    ValueLabel(1, Messages("january")),
    ValueLabel(2, Messages("february")),
    ValueLabel(3, Messages("march")),
    ValueLabel(4, Messages("april")),
    ValueLabel(5, Messages("may")),
    ValueLabel(6, Messages("june")),
    ValueLabel(7, Messages("july")),
    ValueLabel(8, Messages("august")),
    ValueLabel(9, Messages("september")),
    ValueLabel(10, Messages("october")),
    ValueLabel(11, Messages("november")),
    ValueLabel(12, Messages("december"))
  ))

  def buildAccountTitle: AccountTitleObject = AccountTitleObject(Messages("header_account_title"), Messages("blank_name_account_title"))

  def buildServiceDownMessage: Map[String, String] = Map(SERVICE_DOWN_KEY -> Messages("service_down_msg"))
  def buildHomePageLink: HomePageLink = {
    HomePageLink(Messages("home_page_link"))
  }

  def buildResetPasswordTitle: ResetPasswordTitleObject = ResetPasswordTitleObject(Messages("header_account_title"), Messages("reset_password_is_account_cleared_message"))

  def addAnotherPayment = AddAnotherWebsiteModel(buildAddAnotherPaymentMessages)

  def createPaymentMethodInfoModel(payment_methods: Seq[PaymentMethodModel]): Seq[PaymentMethodInfoModel] = payment_methods.map { e => PaymentMethodInfoModel.populateAllowedOperation(e) }

  def getHeader(addFavoritesTab: Boolean = false, saksPageEnabled: Boolean = false): List[Header] =
    List(
      Header(ACCOUNT_SUMMARY_HEADER_TAB_ID, false, Messages(ACCOUNT_SUMMARY_TAB_NAME)),
      Header(ORDER_HISTORY_HEADER_TAB_ID, false, Messages(ORDER_HISTORY_TAB_NAME)),
      Header(SHIPPING_ADRERSS_HEADER_TAB_ID, false, Messages(SHIPPING_ADDRESS_TAB_NAME)),
      Header(BILLING_ADDRESS_HEADER_TAB_ID, false, Messages(BILLING_ADDRESS_TAB_NAME)),
      Header(PAYMENT_HEADER_TAB_ID, false, Messages(PAYMENT_TAB_NAME))
    ) ++
      (
        if (configHelper.banner.equals(Banners.Saks) && saksPageEnabled)
          List(Header(REWARDS_HEADER_TAB_ID, false, Messages(SAKSFIRST_TAB_NAME)))
        else
          List.empty
      ) ++
        (
          if (addFavoritesTab)
            List(Header(ACCOUNT_SETTINGS_HEADER_TAB_ID, false, Messages(ACCOUNT_SETTINGS_TAB_NAME)), Header(FAVORITES_HEADER_TAB_ID, false, Messages(FAVORITES_TAB_NAME)))
          else
            List(Header(ACCOUNT_SETTINGS_HEADER_TAB_ID, false, Messages(ACCOUNT_SETTINGS_TAB_NAME)))
        )

  def buildUserAccountLinks(accountId: String): Map[String, String] = {
    Map(
      EDIT_ACTION_KEY -> controllers.routes.AccountsController.updateAccount(accountId).url,
      ACCOUNT_RESOURCE_KEY -> controllers.routes.AccountsController.getAccount(accountId).url
    )
  }

  def buildRegisterLinks(headers: Map[String, String]): Map[String, String] = Map(
    TERMS_CONDITION_LINK_KEY -> Messages(TERMS_CONDITION_LINK),
    CREATE_ACCOUNT_KEY -> controllers.routes.AccountsController.createAccount().url
  )

  def buildSignInLinks: Option[Map[String, String]] = Some(
    Map(
      SIGN_IN_ACTION_KEY -> controllers.routes.SignInController.signInAction().url
    )
  )

  def buildSignInCsrLinks: Option[Map[String, String]] = Some(
    Map(
      SIGN_IN_ACTION_KEY -> controllers.routes.SignInController.signInCsrAction().url
    )
  )

  def buildLockedAccountLabelLinks = Option(
    Map(
      CONTINUE_SHOPPING_BUTTON_KEY -> Messages("home_page_link")
    )
  )

  def buildPaymentMethodLinks(resource: String) = {
    Option(Map(
      PAYMENT_RESOURCE_LINK_KEY -> resource
    ))
  }

  def buildMoreLinks = None

  val webSiteSaksFirstUrl = "https://" + configHelper.getStringProp("website-host") + Messages("saksfirst_url")
  val mobileSaksFirstUrl = "https://" + configHelper.getStringProp("mobile-host") + Messages("mobile_saksfirst_url")
  val checkWebsiteSaksFirstUrl = "https://" + configHelper.getStringProp("website-host") + Messages("check_saksfirst_url")

  def buildSaksFirstSummaryLinks(headers: Map[String, String], accountId: Option[String]) = {
    Option(
      Map(
        SAKS_FIRST_LANDING_PAGE_KEY -> getSaksFirstURL(headers),
        SAKS_FIRST_POINTS_PAGE_KEY -> Messages(REWARDS_URL)
      )
    )
  }

  def buildSaksFirstSummaryPageLinks(accountId: String, headers: Map[String, String]) = {
    Option(
      Map(
        SAKSFIRST_TERMS -> getSaksFirstTermsUrl(headers),
        TERMS -> getSaksFirstTermsUrl(headers),
        EMAIL_LABEL -> Messages(CONTACT_US_LINK),
        STORE_CARD -> Messages(STORE_CARD_URL),
        ELITE_MASTERCARD -> Messages(MASTERCARD_URL),
        LEARN_MORE -> Messages(LEARN_MORE_URL),
        BEAUTY_TERMS -> Messages(BEAUTY_TERMS_URL),
        SHOP_BEAUTY -> Messages(SHOP_BEAUTY_URL),
        REDEMPTION_LINK_KEY -> controllers.routes.RewardsController.redeem(accountId).url,
        APPLY_NOW -> Messages(APPLY_NOW_URL),
        LEARN_MORE_LABEL_KEY -> getSaksFirstURL(headers),
        BEAUTY_EMAIL_DETAILS -> controllers.routes.RewardsController.sendBeautyBoxEmail(accountId).url
      )
    )
  }

  def buildSaksFirstLinks(headers: Map[String, String], accountId: String) = {
    Option(
      Map(
        PROGRAM_DETAILS_LABEL_LINK_KEY -> getSaksFirstURL(headers),
        CHECK_POINTS_BUTTON_KEY -> Messages(REWARDS_URL),
        SAKSFIRST_STORE_CARD_LINK -> Messages(STORE_CARD_URL),
        SAKSFIRST_MASTER_CARD_LINK -> Messages(MASTERCARD_URL),
        REDEEM_POINTS_LINK_KEY -> Messages(REWARDS_URL),
        LINK_TO_SAKSFIRST_ACTION_KEY -> controllers.routes.RewardsController.linkSaksFirst(accountId).url,
        PAYMENT_RESOURCE_LINK_KEY -> controllers.routes.PaymentMethodController.createPaymentMethod(accountId).url
      )
    )
  }

  private def getSaksFirstURL(headers: Map[String, String]) = {
    if (isFullSiteRequest(headers)) webSiteSaksFirstUrl else mobileSaksFirstUrl
  }

  private def getSaksFirstTermsUrl(headers: Map[String, String]) = {
    if (isFullSiteRequest(headers)) Messages(SAKSFIRST_TERMS_CONDITION_URL) else Messages(MOBILE_SAKSFIRST_TERMS_CONDITION_URL)
  }

  def isFullSiteRequest(headers: Map[String, String]): Boolean = {
    val mobileHostPrefix: Seq[String] = Seq("mobile", "m.", "m-")

    val hostname = headers.toList.map(x => (x._1.toLowerCase(), x._2)).find(x => x._1.equals("x-forwarded-host")) match {
      case Some(x) => x._2
      case _       => ""
    }

    Logger.info(s"Determining mobileHostPrefix: $mobileHostPrefix ")
    Logger.info(s"Determining hostname: $hostname")
    !mobileHostPrefix.exists(p => hostname.contains(p))
  }

  def buildCancelOrderLink(resource: String): Option[Map[String, String]] = {
    Some(Map(
      START_SHOPPING_BUTTON_KEY -> Messages("home_page_link"),
      VIEW_MORE_ORDERS_ACTION -> "",
      CANCEL_ORDER_ACTION -> cancelOrderUrl()
    ))
  }

  def buildViewMoreOrderLinks(resource: String): Option[Map[String, String]] = {
    Some(Map(
      START_SHOPPING_BUTTON_KEY -> Messages("home_page_link"),
      VIEW_MORE_ORDERS_ACTION -> resource,
      CANCEL_ORDER_ACTION -> cancelOrderUrl()
    ))
  }

  def cancelOrderUrl(): String = controllers.routes.OrderController.cancelOrder(order_id = "").url

  def buildPwdSettingsLinks(accountId: String): Option[Map[String, String]] = {
    Some(Map(
      EDIT_ACTION_KEY -> controllers.routes.AccountSettingsController.changePasswordAction(accountId).url
    ))
  }

  def buildEmailPreferencesLinks(accountId: String): Option[Map[String, String]] = {
    Some(Map(EDIT_ACTION_KEY -> controllers.routes.AccountSettingsController.updateAccountSettings(accountId).url))
  }

  def buildForgotPasswordLinks = Map(
    FORGOT_PASSWORD_ACTION_KEY -> forgotPasswordUrl,
    CONTINUE_SHOPPING_BUTTON_KEY -> Messages("home_page_link")
  )

  def buildPasswordResetLinks(loc: String): Map[String, String] = {
    Map(
      RESET_PASSWORD_ACTION_KEY -> resetPasswordUrl(loc),
      CONTINUE_SHOPPING_BUTTON_KEY -> Messages("home_page_link")
    )
  }

  def buildRegisterMessages = Map(
    TITLE_KEY -> Messages("create_account_title"),
    CREATE_ACCOUNT_PAGE_LEVEL_MESSAGE_KEY -> Messages("create_acct_page_level_msg"),
    FIRST_NAME_LABEL_KEY -> Messages("create_account_first_name"),
    LAST_NAME_LABEL_KEY -> Messages("create_account_last_name"),
    EMAIL_LABEL_KEY -> Messages("create_account_email"),
    PASSWORD_LABEL_KEY -> Messages("create_account_password"),
    CONFIRM_PASSWORD_LABEL_KEY -> Messages("create_account_confirm_pwd"),
    PHONE_NUMBER_LABEL_KEY -> Messages("create_account_phone"),
    POSTAL_CODE_LABEL_KEY -> Messages("create_account_zip_code"),
    OPTONAL_LABEL_KEY -> Messages("create_account_optional_label"),
    CANADIAN_CUSTOMER_QUESTION_KEY -> Messages("create_account_canadian_customer_question_label"),
    TERMS_CONDITION_LINK_KEY -> Messages("create_account_terms_condition_label"),
    CANCEL_LINK_KEY -> Messages("create_account_cancel_button_label"),
    SUBMIT_ACCOUNT_BUTTON_KEY -> Messages("create_account_button_label"),
    EMAIL_SUBSCRIPTIONS_KEY -> Messages("create_account_email_updates_label"),
    CANADIAN_TERMS_OF_SERVICE_KEY -> Messages("canadian_customer_terms_of_service", Messages("banner_name_canadian_customer"), Messages("banner_canadian_name_canadian_customer")),
    HTML_CANADIAN_PRIVACY_POLICY_KEY -> Messages(
      "canadian_customer_privacy_policy",
      Messages("banner_name_canadian_customer_privacy_policy"),
      Messages("url_canadian_customer_privacy_policy"),
      Messages("email_canadian_customer_privacy_policy"),
      Messages("website_canadian_customer_privacy_policy")
    ),
    VALIDATION_FIRST_NAME_KEY -> Messages("invalid_first_name"),
    VALIDATION_LAST_NAME_KEY -> Messages("invalid_last_name"),
    VALIDATION_EMAIL_KEY -> Messages("invalid_email"),
    VALIDATION_EQUAL_PWD_KEY -> Messages("invalid_equal_password"),
    VALIDATION_PASSWORD_KEY -> Messages("invalid_password"),
    VALIDATION_POSTAL_CODE_KEY -> Messages("invalid_postal_code"),
    VALIDATION_PHONE_NUMBER_KEY -> Messages("invalid_phone_number"),
    VALIDATION_REQUIRED_FIELD_KEY -> Messages("create_account_required_field")
  )

  def buildForgotPasswordMessages() = Option(Map(
    ACTIVE_TITLE_KEY -> Messages("forgot_password_active_title"),
    INACTIVE_TITLE_KEY -> Messages("forgot_password_inactive_title"),
    MIGRATED_TITLE_KEY -> Messages("forgot_password_migrated_title"),
    SUCCESS_TITLE_KEY -> Messages("forgot_password_success_title"),
    EMAIL_LABEL_KEY -> Messages("forgot_password_email_label"),
    CONTINUE_BUTTON_LABEL_KEY -> Messages("forgot_password_continue_button_label"),
    BACK_TO_SIGN_IN_LINK_KEY -> Messages("forgot_password_back_to_sign_in_link"),
    REQUIRED_FIELD_ERROR_KEY -> Messages("forgot_password_required_field_error"),
    INVALID_EMAIL_ERROR_KEY -> Messages("forgot_password_invalid_email_error"),
    ACTIVE_FORGOT_PASSWORD_MESSAGE_KEY -> Messages("forgot_password_active_message"),
    INACTIVE_FORGOT_PASSWORD_MESSAGE_KEY -> Messages("forgot_password_inactive_message"),
    MIGRATED_FORGOT_PASSWORD_MESSAGE_KEY -> Messages("forgot_password_migrated_message"),
    PASSWORD_RESET_KEY -> Messages("password_reset_message"),
    CONTINUE_SHOPPING_BUTTON_KEY -> Messages("password_reset_continue_shopping_button"),
    SERVICE_DOWN_KEY -> Messages("service_down_msg")
  ))

  def buildPasswordResetMessages() = Option(Map(
    TITLE_KEY -> Messages("reset_password_title"),
    RESET_PASSWORD_LABEL_KEY -> Messages("reset_password_message"),
    NEW_PASSWORD_LABEL_KEY -> Messages("password_reset_new_password_label"),
    CONFIRM_PASSWORD_LABEL_KEY -> Messages("password_reset_confirm_password_label"),
    REQUIRED_FIELD_LABEL_KEY -> Messages("password_reset_required_field"),
    INVALID_EQUAL_PASSWORD_KEY -> Messages("password_reset_invalid_equal_password"),
    INVALID_PASSWORD_LABEL_KEY -> Messages("password_reset_invalid_password"),
    SUCCESS_RESET_PASSWORD_MESSAGE_KEY -> Messages("success_reset_password_message"),
    MY_ACCOUNT_BUTTON_KEY -> Messages("password_reset_my_account_button"),
    LINK_EXPIRED_TITLE_KEY -> Messages("password_reset_link_expired_title"),
    LINK_EXPIRED_MESSAGE_KEY -> Messages("password_reset_link_expired_message"),
    RESET_PASSWORD_BUTTON_KEY -> Messages("reset_password_button"),
    LINK_EXPIRED_BUTTON_KEY -> Messages("link_expired_button"),
    CONTINUE_SHOPPING_BUTTON_KEY -> Messages("continue_shopping_button")
  ))

  def buildSignInMessages() = Option(Map(
    TITLE_KEY -> Messages("sign_in_user_title"),
    EMAIL_LABEL_KEY -> Messages("sign_in_user_email_label"),
    PASSWORD_LABEL_KEY -> Messages("sign_in_user_password_label"),
    REQUIRED_FIELD_ERROR_KEY -> Messages("sign_in_user_required_field_error_label"),
    INVALID_EMAIL_ERROR_KEY -> Messages("sign_in_user_invalid_email_error_label"),
    INVALID_CREDENTIALS_ERROR_KEY -> Messages("sign_in_user_invalid_credentials_error_label"),
    SIGN_IN_BUTTON_KEY -> Messages("sign_in_user_button_label"),
    FORGOT_PASSWORD_LINK_KEY -> Messages("sign_in_user_forgot_password_label")
  ))

  def buildSignInCsrMessages() = Option(Map(
    TITLE_KEY -> Messages("sign_in_csr_title"),
    EMAIL_LABEL_KEY -> Messages("sign_in_csr_user_email_label"),
    PASSWORD_LABEL_KEY -> Messages("sign_in_user_password_label"),
    REQUIRED_FIELD_ERROR_KEY -> Messages("sign_in_user_required_field_error_label"),
    INVALID_EMAIL_ERROR_KEY -> Messages("sign_in_user_invalid_email_error_label"),
    INVALID_CREDENTIALS_ERROR_KEY -> Messages("sign_in_user_invalid_credentials_error_label"),
    SIGN_IN_BUTTON_KEY -> Messages("sign_in_user_button_label"),
    FORGOT_PASSWORD_LINK_KEY -> Messages("sign_in_user_forgot_password_label")
  ))

  def buildAccountLockedMessages() = Option(Map(
    TITLE_KEY -> Messages("account_locked_title"),
    FIRST_MESSAGE_KEY -> Messages("account_locked_first_message"),
    SECOND_MESSAGE_KEY -> Messages("account_locked_second_message", Messages("customer_service_number")),
    CONTINUE_SHOPPING_BUTTON_KEY -> Messages("password_reset_continue_shopping_button")
  ))

  def buildCreateAccountMessages = Option(Map(
    TITLE_KEY -> Messages("sign_in_create_account_title"),
    CREATE_BUTTON_KEY -> Messages("sign_in_create_account_button_label")
  ))

  def buildCreateAccountCsrMessages = Option(Map(
    TITLE_KEY -> Messages("sign_in_csr_create_account_title"),
    CREATE_BUTTON_KEY -> Messages("sign_in_create_account_button_label")
  ))

  def buildUserAccountMessages = Option(Map(
    TITLE_KEY -> Messages("account_profile_title"),
    MODAL_TITLE_KEY -> Messages("account_profile_modal_title"),
    EDIT_BUTTON_KEY -> Messages("account_profile_edit_button_label"),
    FIRST_NAME_LABEL_KEY -> Messages("account_profile_first_name"),
    LAST_NAME_LABEL_KEY -> Messages("account_profile_last_name"),
    EMAIL_LABEL_KEY -> Messages("account_profile_email"),
    PHONE_NUMBER_LABEL_KEY -> Messages("account_profile_phone"),
    CANCEL_LINK_KEY -> Messages("account_profile_cancel_link"),
    SUBMIT_BUTTON_LABEL_KEY -> Messages("account_profile_submit_button"),
    REQUIRED_FIELD_ERROR_KEY -> Messages("required_field"),
    FIRST_NAME_VALIDATION_ERROR_KEY -> Messages("first_name_validation_error"),
    LAST_NAME_VALIDATION_ERROR_KEY -> Messages("last_name_validation_error"),
    EMAIL_VALIDATION_ERROR_KEY -> Messages("email_validation_error"),
    EDIT_YOUR_PROFILE_TITLE -> Messages("edit_your_profile_title"),
    EDIT_YOUR_PROFILE_TEXT -> Messages("edit_your_profile_text")
  ))

  def yesRadioButton = Messages("create_account_yes_radio_button")
  def noRadioButton = Messages("create_account_no_radio_button")

  def buildMoreMessages = Option(Map(
    TITLE_KEY -> Messages("more_rewards_title"),
    MORE_LABEL_KEY -> Messages("more_number_label"),
    SERVICE_DOWN_KEY -> Messages("rewards_service_down_msg"),
    TRY_LATER_KEY -> Messages("try_later_msg")
  ))

  def buildRewardsServiceDownMessages = Option(Map(
    SERVICE_DOWN_KEY -> Messages("rewards_service_down_msg"),
    TRY_LATER_KEY -> Messages("try_later_msg")
  ))

  def migratingSaksFirstError = ApiErrorModel(Messages("migrating_saks_first"), "global_message.saks_first_number")

  def buildSaksFirstMessages = Option(Map(
    TITLE_KEY -> Messages("saks_first_rewards_title"),
    LINK_TO_SAKSFIRST_BUTTON_LABEL_KEY -> Messages("link_saksfirst_button"),
    CHECK_POINTS_BUTTON_KEY -> Messages("check_points_button"),
    PROGRAM_DETAILS_LABEL_LINK_KEY -> Messages("program_details_link"),
    LINK_YOUR_SAKSFIRST_ACCOUNT_LABEL_KEY -> Messages("link_your_saksfirst_label"),
    SAKSFIRST_HTML_PAGELEVEL_MESSAGE -> Messages("saksfirst_page_level_message"),
    POSTAL_CODE_LABEL -> Messages("saksfirst_postal_code"),
    SAKSFIRST_ACCOUNT_NUMBER_LABEL_KEY -> Messages("saksfirst_credit_card"),
    CANCEL_BUTTON_KEY -> Messages("saksfirst_cancel_button_label"),
    SUBMIT_BUTTON_KEY -> Messages("saksfirst_submit_button_label"),
    VALIDATION_POSTAL_CODE_KEY -> Messages("saksfirst_invalid_postal_code"),
    INVALID_CREDIT_CARD -> Messages("saksfirst_invalid_credit_card"),
    REQUIRED_FIELD_ERROR_KEY -> Messages("saksfirst_required_field_error"),
    SERVICE_DOWN_KEY -> Messages("rewards_service_down_msg"),
    TRY_LATER_KEY -> Messages("try_later_msg"),
    FIND_MY_SAKSFIRST_NUMBER_LABEL -> Messages("find_my_saksfirst_number_label"),
    BACK_OF_SAKS_MASTERCARD_LABEL_KEY -> Messages("back_of_saks_mastercard_label"),
    NAME_LABEL_KEY -> Messages("saksfirst_name"),
    VALIDATION_FIRST_NAME_KEY -> Messages("saksfirst_invalid_name"),
    CHECK_YOUR_POINTS_LABEL_KEY -> Messages("check_your_points_label"),
    VIEW_POINTS_LABEL_KEY -> Messages("view_points_label"),
    SAKSFIRST_INFO_PART_ONE_LABEL_KEY -> Messages("saksfirst_info_part_one_label"),
    SAKSFIRST_INFO_PART_TWO_LABEL_KEY -> Messages("saksfirst_info_part_two_label"),
    ADD_YOUR_ACCOUNT_LABEL_KEY -> Messages("add_your_account_label"),
    NOT_A_MEMBER_LABEL_KEY -> Messages("not_a_member_label"),
    LEARN_MORE_LABEL_KEY -> Messages("learn_more_label"),
    SAKSFIRST_POINTS_LABEL -> Messages("saksfirst_points_label"),
    MEMBER_LABEL -> Messages("member_label"),
    REWARDS_DETAILS_LABEL -> Messages("rewards_details_label"),
    REDEEM_POINTS_LABEL -> Messages("redeem_points_label"),
    SAKSFIRST_STORE_CARD_LABEL -> Messages("saksfirst_store_card_label"),
    SAKSFIRST_MASTER_CARD_LABEL -> Messages("saksfirst_master_card_label"),
    PAY_YOUR_BILL_LABEL -> Messages("pay_your_bill_label")
  ))

  def buildSaksFirstSummaryMessages = Option(Map(
    POINTS_AVAILABLE_LABEL_KEY -> Messages("points_available_label"),
    TIER_STATUS_LABEL_KEY -> Messages("tier_status_label"),
    TIER_STATUS_HEADER_LABEL_KEY -> Messages("tier_status_header_label"),
    BANNER_TITLE_LABEL_KEY -> Messages("banner_title_label"),
    BANNER_LINK_LABEL_KEY -> Messages("banner_link_label"),
    GET_GIFT_CARD_LABEL_KEY -> Messages("get_gift_card_label"),
    CURRENT_POINT_BALANCE_LABEL_KEY -> Messages("current_point_balance_label"),
    POINTS_TO_NEXT_REWARD_LABEL_KEY -> Messages("points_to_next_reward_label"),
    AVAILABLE_GIFT_CARD_LABEL -> Messages("available_gift_card_label"),
    REWARDS_SUMMARY_LABEL -> Messages("rewards_summary_label"),
    EARN_POINTS_AND_REWARDS -> Messages("earn_points_and_rewards_msg"),
    MANAGE_POINTS_ONLINE -> Messages("manage_points_online_msg"),
    LEARN_MORE_LABEL_KEY -> Messages("learn_more_label"),
    TOTAL_ORDER_POINTS_KEY -> Messages("total_order_points_msg")
  ))

  def buildSaksFirstSummaryPageMessages = Option(Map(
    REDEEM_POINTS_LABEL -> Messages("redeem_points_label"),
    AVAILABLE_NOW -> Messages("available_now"),
    REDEEM_TITLE -> Messages("redeem_title"),
    TIER_MEMBER -> Messages("tier_member"),
    MEMBER_STATUS -> Messages("member_status"),
    YOUR_POINTS -> Messages("your_points"),
    NEXT_REWARD -> Messages("next_reward"),
    YOUR_REMAINING_BALANCES -> getRemainingBalancesMessage,
    REDEEM_YOUR_GIFT_CARD -> Messages("redeem_your_gift_card"),
    REDEEM_YOUR_POINTS -> Messages("redeem_your_points"),
    VIEW -> Messages("view"),
    REDEEM_FOR_GIFT_CARD -> Messages("redeem_for_gift_card"),
    RECEIVE_GIFT_CARD -> Messages("receive_gift_card"),
    EMAIL_CHANGE -> Messages("email_change"),
    MAIL_CHANGE -> Messages("mail_change"),
    GIFT_CARD_BALANCE -> Messages("gift_card_balance"),
    GIFT_CARD_HISTORY -> Messages("gift_card_history"),
    NO_GIFT_CARD_HISTORY -> Messages("no_gift_card_history"),
    EQUALS_POINTS -> Messages("equals_points"),
    MAILING_ADDRESS -> Messages("mailing_address"),
    POINTS_AVAILABLE_LABEL_KEY -> Messages("points_available_label"),
    BY_EMAIL -> Messages("by_email"),
    EMAIL_LABEL -> Messages("email"),
    BY_MAIL -> Messages("by_mail"),
    ANNUAL_SPEND -> Messages("annual_spend"),
    SAKSFIRST_GIFT_CARD -> Messages("saks_first_gift_card"),
    POINTS -> Messages("points"),
    BACK_TO_YOUR_POINTS -> Messages("back_to_your_points"),
    SUCCESS -> Messages("success"),
    SUCCESS_EMAIL -> Messages("success_email"),
    SUCCESS_MAIL -> Messages("success_mail"),
    ANNUAL_SPEND -> Messages("annual_spend"),
    TIER_INFO -> Messages("tier_info"),
    EMAIL_RECEIPT -> Messages("email_receipt"),
    PAY_YOUR_BILL -> Messages("pay_your_bill"),
    PROGRAM_DETAIL -> Messages("program_details"),
    STORE_CARD -> Messages("store_card"),
    ELITE_MASTERCARD -> Messages("elite_mastercard"),
    NEED_HELP -> Messages("need_help"),
    SHOP_NOW -> Messages("shop_now"),
    TERMS -> Messages("terms"),
    PHONE -> Messages("phone"),
    GIFT_CARD -> Messages("gift_card"),
    BALANCE -> Messages("balance"),
    TOTAL -> Messages("total"),
    NOT_SAKSFIRST_USER_TITLE -> Messages("not_saksfirst_user_title"),
    NOT_SAKSFIRST_USER_BODY -> Messages("not_saksfirst_user_body"),
    APPLY_NOW -> Messages("apply_now"),
    LEARN_MORE_LABEL_KEY -> Messages("learn_more_label"),
    SERVICE_DOWN_KEY -> Messages("service_down_msg"),
    BEAUTY_JOIN_MESSAGE -> Messages("beauty_join_message"),
    CONGRATULATIONS -> Messages("congratulations"),
    BEAUTY_REWARDS_AVAILABLE -> Messages("beauty_rewards_available"),
    EARN_MORE_REWARDS -> Messages("earn_more_rewards"),
    SPEND_MORE -> Messages("spend_more"),
    EARN_MORE_REWARDS_BODY -> Messages("earn_more_rewards_body"),
    REDEEMED_ALL_REWARDS -> Messages("redeemed_all_rewards"),
    SHOP_BEAUTY -> Messages("shop_beauty"),
    BEAUTY_REWARDS_TITLE -> Messages("beauty_rewards_title"),
    BEAUTY_REWARDS_PS -> Messages("beauty_rewards_ps"),
    BEAUTY_REWARDS_MESSAGE -> Messages("beauty_rewards_message"),
    SAKSFIRST_TERMS -> Messages("saksfirst_terms"),
    BEAUTY_AVAILABLE_REWARDS -> Messages("beauty_available_rewards"),
    BEAUTY_REDEMPTION_CODE -> Messages("beauty_redemption_code"),
    BEAUTY_PIN_NUMBER -> Messages("beauty_pin_number"),
    BEAUTY_EMAIL_DETAILS -> Messages("beauty_email_details"),
    VIEW_BEAUTY_REWARDS -> Messages("view_beauty_rewards"),
    BEAUTY_TERMS -> Messages("beauty_terms"),
    BEAUTY_EMAIL_CONFIRMATION -> Messages("beauty_email_confirmation")
  ))

  private def getRemainingBalancesMessage = {
    (Messages("your_remaining_balances") + " " + DateTime.now.getYear.toString)
  }

  def buildCanadianCustomerModel(): CanadianCustomerModel = {
    new CanadianCustomerModel(Seq("saks_canada_opt_status", "off5th_canada_opt_status"), "F", Seq(CanadianCustomerOption("T", yesRadioButton), CanadianCustomerOption("F", noRadioButton)))
  }

  def buildShippingAddressMessages = Option(Map(
    ADDRESS_TITLE -> Messages("address_title"),
    DEFAULT_ADDRESS_TYPE_LABEL -> Messages("default_shipping_address_type_label"),
    DEFAULT_BILLING -> Messages("default_billing"),
    DEFAULT_SHIPPING -> Messages("default_shipping"),
    EDIT_LABEL -> Messages("edit_label"),
    DELETE_LABEL -> Messages("delete_label"),
    ADD_ADDRESS_TITLE -> Messages("add_shipping_address_title"),
    EDIT_ADDRESS_TITLE -> Messages("edit_shipping_address_title"),
    DELETE_ADDRESS_TITLE -> Messages("delete_shipping_address_title"),
    DELETE_ADDRESS_MESSAGE -> Messages("delete_shipping_address_message"),
    CONFIRM_ADDRESS_TITLE -> Messages("confirm_shipping_address_title"),
    CONFIRM_ADDRESS_MESSAGE -> Messages("confirm_shipping_address_message"),
    ENTERED_ADDRESS_SUB_TITLE -> Messages("entered_address_sub_title"),
    ENTERED_ADDRESS_BUTTON -> Messages("entered_address_button"),
    SUGGESTED_ADDRESS_SUB_TITLE -> Messages("suggested_address_sub_title"),
    SUGGESTED_ADDRESS_BUTTON -> Messages("suggested_address_button"),
    STREET_NUMBER_SUB_TITLE -> Messages("street_number_sub_title"),
    STREET_NUMBER_LABEL -> Messages("street_number_label"),
    POTENTIAL_MATCH_LINK -> Messages("potential_match_link"),
    POTENTIAL_MATCH_TITLE -> Messages("potential_match_title"),
    POTENTIAL_MATCH_LABEL -> Messages("potential_match_label"),
    FIRST_NAME_LABEL_KEY -> Messages("first_name_label"),
    LAST_NAME_LABEL_KEY -> Messages("last_name_label"),
    COMPANY_LABEL -> Messages("company_label"),
    ADDRESS_1_LABEL -> Messages("address_1_label"),
    ADDRESS_2_LABEL -> Messages("address_2_label"),
    POSTAL_CODE_LABEL -> Messages("postal_code_label"),
    CITY_LABEL -> Messages("city_label"),
    STATE_PROVINCE_LABEL -> Messages("state_province_label"),
    REGION_PROVINCE_LABEL -> Messages("region_province_label"),
    PHONE_NUMBER_LABEL -> Messages("phone_number_label"),
    CANCEL_LINK -> Messages("cancel_link"),
    SUBMIT_BUTTON -> Messages("submit_button"),
    DELETE_BUTTON -> Messages("delete_button"),
    OPTIONAL_LABEL -> Messages("optional_label"),
    VIEW_MORE_BUTTON -> Messages("view_more_button"),
    SERVICE_DOWN_KEY -> Messages("service_down_msg"),
    FIRST_NAME_VALIDATION_ERROR_KEY -> Messages("account.address.firstname.invalid"),
    LAST_NAME_VALIDATION_ERROR_KEY -> Messages("account.address.lastname.invalid"),
    INVALID_PHONE_NUMBER -> Messages("account.address.phonenumber.invalid"),
    INVALID_FORMAT_ERROR -> Messages("account.address.zip_postalcode.invalid"),
    REQUIRED_FIELD_ERROR_KEY -> Messages("required_field"),
    SECONDARY_MAILING_ADDRESS_ERROR_KEY -> Messages("secondary_shipping_address_error")
  ))

  def buildBillingAddressMessages = Option(Map(
    ADDRESS_TITLE -> Messages("address_title"),
    DEFAULT_ADDRESS_TYPE_LABEL -> Messages("default_billing_address_type_label"),
    DEFAULT_BILLING -> Messages("default_billing"),
    DEFAULT_SHIPPING -> Messages("default_shipping"),
    EDIT_LABEL -> Messages("edit_label"),
    DELETE_LABEL -> Messages("delete_label"),
    ADD_ADDRESS_TITLE -> Messages("add_billing_address_title"),
    EDIT_ADDRESS_TITLE -> Messages("edit_billing_address_title"),
    DELETE_ADDRESS_TITLE -> Messages("delete_billing_address_title"),
    DELETE_ADDRESS_MESSAGE -> Messages("delete_billing_address_message"),
    CONFIRM_ADDRESS_TITLE -> Messages("confirm_billing_address_title"),
    CONFIRM_ADDRESS_MESSAGE -> Messages("confirm_billing_address_message"),
    ENTERED_ADDRESS_SUB_TITLE -> Messages("entered_address_sub_title"),
    ENTERED_ADDRESS_BUTTON -> Messages("entered_address_button"),
    SUGGESTED_ADDRESS_SUB_TITLE -> Messages("suggested_address_sub_title"),
    SUGGESTED_ADDRESS_BUTTON -> Messages("suggested_address_button"),
    STREET_NUMBER_SUB_TITLE -> Messages("street_number_sub_title"),
    STREET_NUMBER_LABEL -> Messages("street_number_label"),
    POTENTIAL_MATCH_LINK -> Messages("potential_match_link"),
    POTENTIAL_MATCH_TITLE -> Messages("potential_match_title"),
    POTENTIAL_MATCH_LABEL -> Messages("potential_match_label"),
    FIRST_NAME_LABEL_KEY -> Messages("first_name_label"),
    LAST_NAME_LABEL_KEY -> Messages("last_name_label"),
    COMPANY_LABEL -> Messages("company_label"),
    ADDRESS_1_LABEL -> Messages("address_1_label"),
    ADDRESS_2_LABEL -> Messages("address_2_label"),
    POSTAL_CODE_LABEL -> Messages("postal_code_label"),
    CITY_LABEL -> Messages("city_label"),
    STATE_PROVINCE_LABEL -> Messages("state_province_label"),
    REGION_PROVINCE_LABEL -> Messages("region_province_label"),
    PHONE_NUMBER_LABEL -> Messages("phone_number_label"),
    CANCEL_LINK -> Messages("cancel_link"),
    SUBMIT_BUTTON -> Messages("submit_button"),
    DELETE_BUTTON -> Messages("delete_button"),
    OPTIONAL_LABEL -> Messages("optional_label"),
    VIEW_MORE_BUTTON -> Messages("view_more_button"),
    SERVICE_DOWN_KEY -> Messages("service_down_msg"),
    FIRST_NAME_VALIDATION_ERROR_KEY -> Messages("account.address.firstname.invalid"),
    LAST_NAME_VALIDATION_ERROR_KEY -> Messages("account.address.lastname.invalid"),
    INVALID_PHONE_NUMBER -> Messages("account.address.phonenumber.invalid"),
    INVALID_FORMAT_ERROR -> Messages("account.address.zip_postalcode.invalid"),
    REQUIRED_FIELD_ERROR_KEY -> Messages("required_field"),
    SECONDARY_MAILING_ADDRESS_ERROR_KEY -> Messages("secondary_billing_address_error")
  ))

  def buildOrderMessages = Option(Map(
    TITLE_KEY -> Messages("order_title"),
    NO_ORDER_MESSAGE_KEY -> Messages("order_no_order_message"),
    CHECK_BACK_MESSAGE_KEY -> Messages("order_check_back_message"),
    START_SHOPPING_BUTTON_KEY -> Messages("order_start_shopping_button_message"),
    PAYMENT_METHOD_TITLE_KEY -> Messages("order_payment_method_title"),
    BILLING_ADDRESS_TITLE_KEY -> Messages("order_billing_address_title"),
    GIFT_CARD_CHARGES_TITLE_KEY -> Messages("order_gift_card_charges_title"),
    PROMO_CODE_TITLE_KEY -> Messages("order_promo_code_title"),
    SUMMARY_TITLE_KEY -> Messages("order_summary_title"),
    SUBTOTAL_LABEL_KEY -> Messages("order_subtotal_label"),
    GIFT_WRAP_LABEL_KEY -> Messages("order_gift_wrap_label"),
    SHIPPING_HANDLING_LABEL_KEY -> Messages("order_shipping_handling_label"),
    ORIGINAL_ITEM_TOTAL_LABEL_KEY -> Messages("original_item_total_label"),
    ITEM_TOTAL_LABEL_KEY -> Messages("item_total_label"),
    PROMOTIONAL_SAVINGS_LABEL_KEY -> Messages("order_discount_label"),
    TOTAL_BEFORE_TAX_LABEL_KEY -> Messages("order_total_before_tax_label"),
    TAX_LABEL_KEY -> Messages("order_tax_label"),
    TOTAL_LABEL_KEY -> Messages("order_total_label"),
    YOU_SAVED_LABEL_KEY -> Messages("order_you_saved_label"),
    BILLING_DETAILS_TITLE_KEY -> Messages("order_billing_details_title"),
    EXP_DATE_LABEL_KEY -> Messages("order_exp_date_label"),
    GIFT_CARD_LABEL_KEY -> Messages("order_gift_card_label"),
    GIFT_CARD_TOTAL_LABEL_KEY -> Messages("order_gift_card_total_label"),
    SHIPPING_TO_THIS_ADDRESS_TITLE_KEY -> Messages("order_shipping_to_this_address_title"),
    SHIPPING_ADDRESS_TITLE_KEY -> Messages("order_shipping_address_title"),
    SHIPPING_METHOD_TITLE_KEY -> Messages("order_shipping_method_title"),
    ITEM_NUM_TITLE_KEY -> Messages("order_item_num_title"),
    ITEMS_NUM_TITLE_KEY -> Messages("order_items_num_title"),
    STANDARD_DELIVERY_LABEL_KEY -> Messages("order_standard_delivery_label"),
    RUSH_DELIVERY_LABEL_KEY -> Messages("order_rush_delivery_label"),
    EST_DELIVERY_LABEL_KEY -> Messages("order_est_delivery_label"),
    EST_DELIVERY_NOT_AVAILABEL_MESSAGE_KEY -> Messages("order_est_delivery_not_availabel_message"),
    ELECTRONIC_DELIVERY_TITLE_KEY -> Messages("order_electronic_delivery_title"),
    ITEM_TITLE_KEY -> Messages("order_item_title"),
    UNIT_PRICE_TITLE_KEY -> Messages("order_unit_price_title"),
    GIFT_OPTIONS_TITLE_KEY -> Messages("order_gift_options_title"),
    STATUS_TITLE_KEY -> Messages("order_history_status_title"),
    SIZE_LABEL_KEY -> Messages("order_size_label"),
    COLOR_LABEL_KEY -> Messages("order_color_label"),
    QUANTITY_LABEL_KEY -> Messages("order_quantity_label"),
    WRITE_A_REVIEW_LINK_KEY -> Messages("order_write_a_review_link"),
    FINAL_SALE_MESSAGE_KEY -> Messages("order_final_sale_message"),
    ESTIMATED_DELIVERY_LABEL_KEY -> Messages("order_estimated_delivery_label"),
    TRACK_ITEM_LINK_KEY -> Messages("order_track_item_link"),
    GIFT_MESSAGE_LABEL_KEY -> Messages("order_gift_message_label"),
    FROM_LABEL_KEY -> Messages("order_from_label"),
    TO_LABEL_KEY -> Messages("order_to_label"),
    VIA_EMAIL_LABEL_KEY -> Messages("order_via_email_label"),
    PRINT_RECEIPT_LABEL_KEY -> Messages("order_print_receipt_label"),
    QUESTION_ABOUT_ORDER_MESSAGE_KEY -> Messages("order_question_about_order_message", Messages("customer_service_number")),
    VIEW_MORE_ORDERS_BUTTON_KEY -> Messages("order_view_more_orders_button"),
    VIEW_ALL_ORDERS_LINK_KEY -> Messages("order_view_all_orders_link"),
    PAYPAL_LABEL_KEY -> Messages("order_paypal_label"),
    ORDER_LABEL -> Messages("order_label"),
    NOT_AVAILABLE_MESSSGE -> Messages("order_not_avalibale_message"),
    SHIPS_FROM_STORE_MESSAGE -> Messages("order_ships_from_store_message"),
    TOOL_TIP_MESSAGE -> Messages("store_order_tool_tip_message"),
    STORE_TOOL_TIP_MESSAGE -> Messages("store_order_tool_tip_message"),
    DROPSHIP_TOOL_TIP_MESSAGE -> Messages("dropship_order_tool_tip_message"),
    SHIPS_FROM_VENDOR_MESSAGE -> Messages("order_ships_from_vendor_message"),
    PERSONALIZATION_LABEL -> Messages("order_personalization_message"),
    TEXT_LABEL -> Messages("order_text_label"),
    SIGNATURE_REQUIRED_MESSAGE -> Messages("order_signature_required_message"),
    CANCEL_ORDER_BUTTON -> Messages("order_cancel_order_button"),
    DROPSHIP_LABEL -> Messages("dropship_label"),
    CANCEL_ORDER_COUNTDOWN_MESSAGE -> Messages("order_cancel_order_countdown_message"),
    CANCEL_ORDER_TITLE -> Messages("order_cancel_order_title"),
    CANCEL_ORDER_MESSAGE_ONE -> Messages("order_cancel_order_message_one"),
    CANCEL_ORDER_MESSAGE_TWO -> Messages("order_cancel_order_message_two"),
    CANCEL_THIS_ORDER_BUTTON -> Messages("order_cancel_this_order_button"),
    CANCEL_LINK -> Messages("order_cancel_link"),
    ORDER_CANCELED_MESSAGE -> Messages("order_order_canceled_message", Messages("customer_service_number")),
    ORDER_NOT_CANCELED_TITLE -> Messages("order_order_not_canceled_title"),
    ORDER_NOT_CANCELED_MESSAGE -> Messages("order_order_not_canceled_message", Messages("customer_service_number")),
    CLOSE_BUTTON -> Messages("order_close_button"),
    SERVICE_DOWN_KEY -> Messages("service_down_msg"),
    ORDER_TECHNICAL_ERROR -> Messages("order_technical_msg", Messages("customer_service_number")),
    ORDER_CANCELED_TITLE -> Messages("order_canceled_title"),
    ORDER_STATUS_TITLE -> Messages("order_status_title"),
    LATEST_EXPECTED -> Messages("latest_expected"),
    SHIP_DATE -> Messages("ship_date"),
    PRE_ORDER_MSG_SUFFIX -> Messages("pre_order_msg_suffix"),
    PRE_ORDER_MESSAGE -> Messages("pre_order_message"),
    PICKUP_IN_STORE_EMAIL_NOTIFICATION -> Messages("pickup_in_store_email_notification"),
    PICKUP_IN_STORE_LABEL -> Messages("pickup_in_store_label"),
    STORE_PICKUP_INFORMATION_LABEL -> Messages("store_pickup_information_label"),
    PICKUP_PERSON_LABEL -> Messages("pickup_person_label")
  ))

  def buildCheckOrderStatusMessages() = Map(
    TITLE_KEY -> Messages("check_order_status_title"),
    ORDER_NUMBER_LABEL_KEY -> Messages("order_status_order_number_label"),
    ZIP_CODE_LABEL_KEY -> Messages("order_status_zip_code_label"),
    SUBMIT_BUTTON_LABEL_KEY -> Messages("order_status_button_label"),
    REQUIRED_FIELD_ERROR -> Messages("order_status_required_field_error"),
    INVALID_ORDER_NUMBER -> Messages("order_status_invalid_order_number"),
    INVALID_FORMAT -> Messages("order_status_invalid_format"),
    NO_MATCH -> Messages("order_status_no_match", Messages("customer_service_number"))
  )

  def buildCheckOrderStatusCsrMessages() = Map(
    TITLE_KEY -> Messages("check_order_status_csr_title"),
    ORDER_NUMBER_LABEL_KEY -> Messages("order_status_order_number_label"),
    ZIP_CODE_LABEL_KEY -> Messages("order_status_zip_code_label"),
    SUBMIT_BUTTON_LABEL_KEY -> Messages("order_status_button_label"),
    REQUIRED_FIELD_ERROR -> Messages("order_status_required_field_error"),
    INVALID_ORDER_NUMBER -> Messages("order_status_invalid_order_number"),
    INVALID_FORMAT -> Messages("order_status_invalid_format"),
    NO_MATCH -> Messages("order_status_no_match", Messages("customer_service_number"))
  )

  def buildPaymentMethodMessages = Option(Map(
    ACCOUNT_TITLE_KEY -> Messages("payment_account_title"),
    DELETE_PAYMENT_TITLE_KEY -> Messages("delete_payment_title"),
    ADD_PAYMENT_TITLE_KEY -> Messages("add_payment_title"),
    EDIT_PAYMENT_TITLE_KEY -> Messages("edit_payment_title"),
    DELETE_PAYMENT_QUESTION_KEY -> Messages("delete_payment_question"),
    DELETE_LINK_KEY -> Messages("delete_label"),
    EDIT_LINK_KEY -> Messages("edit_label"),
    CANCEL_LINK_KEY -> Messages("cancel_label"),
    SUBMIT_BUTTON_KEY -> Messages("submit_label"),
    DEFAULT_PMT_LABEL_KEY -> Messages("default_payment_method"),
    DEFAULT_PMT_CHECK_BOX_LABEL_KEY -> Messages("default_payment_checkbox_label"),
    CARD_TYPE_LABEL_KEY -> Messages("cart_type_label"),
    CARD_NUMBER_LABEL_KEY -> Messages("card_number_label"),
    EXPIRATION_DATE_LABEL_KEY -> Messages("expiration_date_label"),
    MONTH_LABEL_KEY -> Messages("month_label"),
    YEAR_LABEL_KEY -> Messages("year_label"),
    SERVICE_DOWN_KEY -> Messages("service_down_msg"),
    EXP_DATE_LABEL_KEY -> Messages("exp_date_label"),
    CARD_ENDING_LABEL_KEY -> Messages("card_ending_label"),
    ENDING_IN_LABEL_KEY -> Messages("ending_label"),
    REQUIRED_FIELD_ERROR_KEY -> Messages("required_field"),
    CARD_NAME_VALIDATION_ERROR_KEY -> Messages("payment_name_invalid"),
    CREDIT_CARD_NUMBER_VALIDATION_ERROR_KEY -> Messages("payment_number_invalid"),
    CARD_NAME_LABEL_KEY -> Messages("card_name_label"),
    POSTAL_CODE_LABEL -> Messages("postal_code_label"),
    VALIDATION_POSTAL_CODE_KEY -> Messages("invalid_postal_code")
  ))

  def buildAddAnotherPaymentMessages = Option(Map(
    ADD_LABEL_MSG_KEY -> Messages("create_payment_method")
  ))

  def buildAddAnotherShippingMessages = Option(Map(
    ADD_LABEL_MSG_KEY -> Messages("create_shipping_address")
  ))

  def buildAddAnotherBillingMessages = Option(Map(
    ADD_LABEL_MSG_KEY -> Messages("create_billing_address")
  ))

  def buildPwdSettingsMessages = Option(Map(
    TITLE_KEY -> Messages("password_settings_title"),
    MODAL_TITLE_KEY -> Messages("pwd_setting_modal_title"),
    PASSWORD_INFO_KEY -> Messages("pwd_setting_password_info"),
    CHANGE_PASSWORD_LINK_KEY -> Messages("change_password_link"),
    OLD_PASSWORD_KEY -> Messages("old_password"),
    NEW_PASSWORD_KEY -> Messages("new_password"),
    CONFIRM_NEW_PASSWORD_LABEL_KEY -> Messages("confirm_new_password"),
    CANCEL_LINK_KEY -> Messages("pwd_setting_cancel_link"),
    SUBMIT_BUTTON_LABEL_KEY -> Messages("pwd_setting_submit_button"),
    REQUIRED_FIELD_ERROR_KEY -> Messages("required_field"),
    INCORRECT_OLD_PASSWORD_ERROR_KEY -> Messages("incorrect_old_password_error"),
    CONFIRM_PASSWORDS_NOT_MATCH_ERROR_KEY -> Messages("confirm_password_match_error"),
    PASSWORD_VALIDATION_ERROR_KEY -> Messages("password_validation_error")
  ))

  def buildEmailPreferencesMessages: Option[Map[String, String]] = Some(Map(
    TITLE_KEY -> Messages("email_preferences_title"),
    MODAL_TITLE_KEY -> Messages("modal_title"),
    SUBSCRIBED_LABEL_KEY -> Messages("subscribed_label"),
    UNSBSCRIBED_LABEL_KEY -> Messages("unsubscribed_label"),
    EDIT_LINK_KEY -> Messages("email_preferences_edit_link"),
    SAKS_EMAIL_PREFR_LABEL_KEY -> Messages("saks_email_preference"),
    OFF_FIFTH_EMAIL_PREFR_LABEL_KEY -> Messages("off5th_email_preference"),
    OFF_FIFTH_CANADA_EMAIL_PREFR_LABEL_KEY -> Messages("off5th_canada_email_preference"),
    SAKS_CANADA_EMAIL_PREFR_LABEL_KEY -> Messages("saks_canada_email_preference"),
    CANADIAN_CUSTOMER_QUESTION_KEY -> Messages("email_prefr_canadian_sutom_question"),
    CANADIAN_TERMS_OF_SERVICE_KEY -> Messages("canadian_customer_terms_of_service", Messages("banner_name_canadian_customer"), Messages("banner_canadian_name_canadian_customer")),
    EDIT_MESSAGE_BLOCK -> Messages("edit_message_block", Messages("marketing_message_block")),
    CANCEL_LINK_KEY -> Messages("email_preferences_cancel_link"),
    SUBMIT_BUTTON_LABEL_KEY -> Messages("email_preferences_submit_button")
  ))

  def buildAccountServiceDownMessage = Map(
    SERVICE_DOWN_KEY -> Messages("service_down_msg")
  )

  def buildAccountGreetingMessages = Option(Map(
    ACCOUNT_TITLE -> Messages("header_account_title"),
    BLANK_NAME_ACCOUNT_TITLE -> Messages("blank_name_account_title")
  ))

  def forgotPasswordUrl: String = controllers.routes.ResetPasswordController.forgotPasswordAction.url

  def resetPasswordUrl(loc: String): String = controllers.routes.ResetPasswordController.resetPasswordAction(loc).url

  // Sample tuple if you want to keep the error code sent back from blue martini.
  // def getOtherDataAndError = (Messages("payment_number_already_exists"), None)
  val emailExistsMessage = Messages(EMAIL_ALREADY_EXISTS_IN_SYSTEM)
  val getCardAlreadyExistDataAndError = (Messages("payment_number_already_exists"), Option("number.invalid_number"))

  val getInvalidCreditCardNumber = (Messages("payment_number_invalid"), Option("number.invalid_number"))

  val getDoesNotMatchRecordDataAndError = (Messages(INVALID_SIGN_IN_MESSAGE), Option(GLOBAL_NOT_MATCH_REGISTERED_ACCOUNT_MSG))

  val getInvalidEmailFormatDataAndError = (Messages(INVALID_EMAIL_FORMAT), None)

  val getInvalidPasswordFormatDataAndError = (Messages(INVALID_PASSWORD_FORMAT), Option(INVALID_PASSWORD_FORMAT_MSG))

  val getInvalidOrderNumberDataAndError = (Messages("order_status_no_match", Messages("customer_service_number")), Option(GLOBAL_MESSAGE_ORDER_NUMBER))

  val getInvalidBillingDataAndError = (Messages("order_status_no_match", Messages("customer_service_number")), Option(GLOBAL_MESSAGE_BILLING_ZIPCODE))

  val getResetLinkExpiredDataAndError = (Messages("reset_password_reset_link_expired_message"), Option(GLOBAL_MESSAGE_RESET_LINK_EXPIRED))

  val getRedemptionDataAndError = (Messages("rewards_service_down_msg"), Option(GLOBAL_MESSAGE_REDEMPTION))

  def getAccountAlreadyExistsDataAndError(resource: Option[String] = None) = {
    resource match {
      case Some("create_account") => (Messages("account_already_exists"), Option(ACCOUNT_ALREADY_EXISTS_MSG))
      case _                      => (Messages("update_email_already_exists"), Option(ACCOUNT_ALREADY_EXISTS_MSG))
    }
  }

  def getBrandSpecificErrorMessage(errors: Seq[ApiErrorModel], resource: Option[String] = None, status: Int = 500): Seq[ApiErrorModel] = {
    errors.map { error =>
      error.error match {
        case DUPLICATE_ADDRESS                                       => ApiErrorModel(Messages("duplicate_address"), s"$GLOBAL_MESSAGE_KEY.$DUPLICATE_ADDRESS")
        case DUPLICATE_CARD_NUM                                      => ApiErrorModel(getCardAlreadyExistDataAndError._1, getCardAlreadyExistDataAndError._2.getOrElse(error.error))
        case INVALID_CREDIT_CARD_NUM                                 => ApiErrorModel(getInvalidCreditCardNumber._1, getInvalidCreditCardNumber._2.getOrElse(error.error))
        case NOT_MATCH_REGISTERED_ACCOUNT                            => ApiErrorModel(getDoesNotMatchRecordDataAndError._1, getDoesNotMatchRecordDataAndError._2.getOrElse(error.error))
        case INVALID_EMAIL_FORMAT                                    => ApiErrorModel(getDoesNotMatchRecordDataAndError._1, getDoesNotMatchRecordDataAndError._2.getOrElse(error.error))
        case ERROR_INVALID_ORDER_NUMBER                              => ApiErrorModel(getInvalidOrderNumberDataAndError._1, getInvalidOrderNumberDataAndError._2.getOrElse(error.error))
        case COULD_NOT_REDEEM                                        => ApiErrorModel(getRedemptionDataAndError._1, getRedemptionDataAndError._2.getOrElse(error.error))

        case SHIP_ADDRESS_INV_DAY_PHONE | BILL_ADDRESS_INV_DAY_PHONE => ApiErrorModel(Messages("phone.error.pattern"), "phone")
        case ERROR_INVALID_BILLING_ZIP_CODE =>
          ApiErrorModel(getInvalidBillingDataAndError._1, getInvalidBillingDataAndError._2.getOrElse(error.error))
        case INVALID_PASSWORD_FORMAT => ApiErrorModel(getInvalidPasswordFormatDataAndError._1, getInvalidPasswordFormatDataAndError._2.getOrElse(error.error))

        case ERROR_EMAIL_ALREADY_EXISTS =>
          error.data match {
            case `emailExistsMessage` =>
              val dataError = getAccountAlreadyExistsDataAndError(resource)
              ApiErrorModel(dataError._1, dataError._2.getOrElse(error.error))
            case _ => error
          }
        case ERROR_INVALID_RESET_LINK_EXPIRED => ApiErrorModel(getResetLinkExpiredDataAndError._1, getResetLinkExpiredDataAndError._2.getOrElse(error.error))
        case LOCKED_USER_ACCOUNT              => ApiErrorModel(error.data, "locked_user_account.error")
        case MIGRATED_USER_ACCOUNT            => ApiErrorModel(error.data, "migrated_user_account.error")
        case EXPIRED_USER_ACCOUNT             => ApiErrorModel(error.data, "inactive_user_account.error")
        case SERVER_ERROR                     => ApiErrorModel(Messages("service_down_msg"), s"$GLOBAL_MESSAGE_KEY.error")
        case _ if status < 500                => error
        case _ =>
          if (configHelper.getBooleanProp(DEV_MODE)) {
            error
          } else {
            ApiErrorModel(GENERIC_ERROR_MESSAGE, GENERIC_ERROR)
          }
      }
    }
  }

  object DefaultPreferences {
    def defaultPreferences(createAccountRequest: CreateAccountRequest): (String, String, Option[String], Option[String]) = {
      val noEmailPreference = ("F", "F", Option("F"), Option("F"))
      val nonCanadianCustomer = (createAccountRequest.saks_opt_status.getOrElse("F"), createAccountRequest.off5th_opt_status.getOrElse("F"), Option("F"), Option("F"))

      def canadianCustomer = configHelper.banner match {
        case Banners.Saks   => ("T", "F", Option("T"), Option("F"))
        case Banners.Off5th => ("F", "T", Option("F"), Option("T"))
        case _              => noEmailPreference
      }

      createAccountRequest.canadian_customer match {
        case Some("T") => createAccountRequest.canadian_customer_opt_in match {
          case Some(true) => canadianCustomer
          case _          => noEmailPreference
        }
        case _ => nonCanadianCustomer
      }

    }
  }
}
