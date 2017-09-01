package models.website

import constants.Banners
import constants.Constants._
import models.Constants._
import models.FieldConstraints
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
case class CanadianCustomerOption(value: String, label: String)

case class CanadianCustomerModel(preferences: Seq[String], value: String, options: Seq[CanadianCustomerOption])

case class EmailOption(value: String, label: String)

object EmailOption {
  implicit val emailOption = Json.format[EmailOption]
}

object AvailableOptions {
  val saksOff5th = EmailOption(Off5thOptStatus, SaksOffFifth_VALUE)
  val saks = EmailOption(SaksOptStatus, SaksFifthAvenue_VALUE)
  val saksOff5thCanada = EmailOption(Off5thCanadaOptStatus, Off5th_CANADA_VALUE)
  val saksCanada = EmailOption(SaksCanadaOptStatus, SaksFifthAvenue_CANADA_VALUE)
  val gilt = EmailOption(GiltOptStatus, Gilt_VALUE)
  val lordAndTaylor = EmailOption(LordAndTaylorOptStatus, LordAndTaylor_VALUE)
}

case class EmailSubscriptionsModel(value: Seq[String], options: Seq[EmailOption])

object EmailSubscriptionsModelBuilder {

  def buildUpdatePreferencesOptions(banner: Banners.Value, preferences: Seq[String]) = {
    banner match {
      case Banners.Off5th =>
        EmailSubscriptionsModel(preferences, Seq(
          AvailableOptions.saksOff5th,
          AvailableOptions.saks,
          AvailableOptions.saksOff5thCanada,
          AvailableOptions.saksCanada
        ))
      case Banners.Saks =>
        EmailSubscriptionsModel(preferences, Seq(
          AvailableOptions.saks,
          AvailableOptions.saksOff5th,
          AvailableOptions.saksCanada,
          AvailableOptions.saksOff5thCanada
        ))
      case Banners.LordAndTaylor =>
        EmailSubscriptionsModel(preferences, Seq(
          AvailableOptions.lordAndTaylor
        ))
    }
  }

  val registerDefaultPreferences = Seq(SaksOptStatus, Off5thOptStatus)

  def buildRegisterOptions(banner: Banners.Value) = {
    banner match {
      case Banners.Off5th =>
        EmailSubscriptionsModel(registerDefaultPreferences, Seq(
          AvailableOptions.saksOff5th,
          AvailableOptions.saks
        ))
      case Banners.Saks =>
        EmailSubscriptionsModel(registerDefaultPreferences, Seq(
          AvailableOptions.saks,
          AvailableOptions.saksOff5th
        ))
      case Banners.LordAndTaylor =>
        EmailSubscriptionsModel(Seq(LordAndTaylorOptStatus), Seq(AvailableOptions.lordAndTaylor))
    }
  }

  def buildGiltRegisterOptions(banner: Banners.Value) = {
    val registerDefaultPreferencesOff5th = Seq(SaksOptStatus, Off5thOptStatus, GiltOptStatus)
    banner match {
      case Banners.Off5th =>
        EmailSubscriptionsModel(registerDefaultPreferencesOff5th, Seq(
          AvailableOptions.saksOff5th,
          AvailableOptions.saks,
          AvailableOptions.gilt
        ))
      case Banners.Saks =>
        EmailSubscriptionsModel(registerDefaultPreferences, Seq(
          AvailableOptions.saks,
          AvailableOptions.saksOff5th
        ))
    }
  }
}

case class RegisterWebsiteModel(
  messages:            Map[String, String],
  links:               Map[String, String],
  canadian_customer:   CanadianCustomerModel,
  email_subscriptions: EmailSubscriptionsModel
)

object RegisterWebsiteModel {
  implicit val canadianCustomerOption = Json.format[CanadianCustomerOption]
  implicit val canadianCustomerModelFormat = Json.format[CanadianCustomerModel]
  implicit val emailSubscriptionsModel = Json.format[EmailSubscriptionsModel]
  implicit val registerWebsiteModelFormat = Json.format[RegisterWebsiteModel]
}

case class GiltEmailSubscriptionModel(
  authentication: GiltAuthentication,
  first_name:     String,
  last_name:      String,
  user_partition: Int                = GILT_USER_PARTITION,
  test_bucket_id: Long               = GILT_TEST_BUCKET_ID,
  promotion_id:   String             = GILT_PROMOTION_ID,
  channel:        String             = GILT_CHANNEL
)

case class GiltAuthentication(
  email_password_authentication: EmailPasswordAuthentication
)

case class EmailPasswordAuthentication(
  email:    String,
  password: String
)

case class CreateAccountRequest(
  first_name:               String,
  last_name:                String,
  password:                 String,
  confirm_password:         String,
  email:                    String,
  canadian_customer:        Option[String],
  canadian_customer_opt_in: Option[Boolean],
  saks_opt_status:          Option[String],
  off5th_opt_status:        Option[String],
  phone_number:             Option[String],
  zip:                      Option[String],
  preferences:              Option[Seq[String]]
)

object GiltEmailSubscriptionModel {
  implicit val giltEmailPasswordAuthenticationModelFormat = Json.format[EmailPasswordAuthentication]
  implicit val giltAuthenticationModelFormat = Json.format[GiltAuthentication]
  implicit val giltEmailSubscriptionModelFormat = Json.format[GiltEmailSubscriptionModel]
}

object CreateAccountRequest extends FieldConstraints {
  implicit val createAccountRequestReads: Reads[CreateAccountRequest] = (
    (__ \ FIRST_NAME).read[String](name) and
    (__ \ LAST_NAME).read[String](name) and
    (__ \ PASSWORD).read[String](password) and
    (__ \ CONFIRM_PASSWORD).read[String](password) and
    (__ \ EMAIL).read[String](email) and
    (__ \ CANADIAN_CUSTOMER).readNullable[String](TorFValue) and
    (__ \ CANADIAN_CUSTOMER_OPT_IN).readNullable[Boolean] and
    (__ \ SAKS_OPT_STATUS).readNullable[String](TorFValue) and
    (__ \ OFF5TH_OPT_STATUS).readNullable[String](TorFValue) and
    (__ \ PHONE_NAMBER).readNullable[String](requiredPhone or empty) and
    (__ \ ZIP).readNullable[String](zip or empty) and
    (__ \ PREFERENCES).readNullable[Seq[String]]
  )(CreateAccountRequest.apply _).flatMap { createAccountRequest =>
      Reads { _ =>
        if (createAccountRequest.password == createAccountRequest.confirm_password) {
          JsSuccess(createAccountRequest)
        } else {
          JsError(JsPath(List(KeyPathNode("confirm_password"))), ValidationError("passwords.must.match"))
        }
      }
    }
  implicit val createAccountRequestWrites = Json.writes[CreateAccountRequest]
}

