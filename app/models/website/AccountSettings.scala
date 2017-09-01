package models.website

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class PasswordSettingModel(
  enabled:  Boolean                     = true,
  links:    Option[Map[String, String]],
  messages: Option[Map[String, String]]
)

case class PreferencesResponseModel(
  preferences: Seq[String]
)

case class EmailPreferencesRequest(preferences: Seq[String]) {
  def toPreferencesRequestModel(headers: Map[String, String], userId: String): PreferencesRequestModel = {
    PreferencesRequestModel(
      if (preferences.contains("off5th_opt_status")) "T" else "F",
      if (preferences.contains("saks_opt_status")) "T" else "F",
      if (preferences.contains("off5th_canada_opt_status")) "T" else "F",
      if (preferences.contains("saks_canada_opt_status")) "T" else "F",
      userId,
      headers.getOrElse("X-Real-IP", ""),
      DateTime.now().toString("yyyy-MM-dd'T'HH:mm:ssZ")
    )
  }
}

object EmailPreferencesRequest {
  implicit val emailPreferencesReads: Reads[EmailPreferencesRequest] =
    (JsPath \ "preferences").read[Seq[String]](Reads.verifying[Seq[String]](_.forall { preference =>
      val options = List("off5th_opt_status", "saks_opt_status", "off5th_canada_opt_status", "saks_canada_opt_status")
      options.contains(preference)
    })).map(EmailPreferencesRequest.apply)

  implicit val emailPreferencesWrites = Json.writes[EmailPreferencesRequest]
}

case class PreferencesRequestModel(
    opt_status:               String = "F",
    saks_opt_status:          String = "F",
    off5th_canada_opt_status: String = "F",
    saks_canada_opt_status:   String = "F",
    email_address:            String,
    ip_address:               String,
    subscribed_date:          String
) {
  implicit val preferencesRequestModelReads = Json.reads[EmailPreferencesRequest]
  implicit val preferencesRequestModelWrites = Json.writes[PreferencesRequestModel]
}

case class EmailPreferencesModel(
    off5th_opt_status:        Option[String] = Some("F"),
    saks_opt_status:          Option[String] = Some("F"),
    off5th_canada_opt_status: Option[String] = Some("F"),
    saks_canada_opt_status:   Option[String] = Some("F")
) {

  def fields = {
    implicit val emailPreferenceParsing = Json.format[EmailPreferencesModel]
    Json.toJson(this).as[JsObject].fields
  }

  def toResponsePayload: Seq[String] = fields.filter(_._2.asOpt[String].contains("T")).map(_._1)
}

case class MarketingSignUpModel(
  email_address:            String,
  subscribed_date:          DateTime,
  saks_opt_status:          String,
  ip_address:               Option[String] = None,
  user_id:                  Option[String] = None,
  first_name:               Option[String] = None,
  middle_name:              Option[String] = None,
  last_name:                Option[String] = None,
  address:                  Option[String] = None,
  address_two:              Option[String] = None,
  city:                     Option[String] = None,
  state:                    Option[String] = None,
  zip:                      Option[String] = None,
  country:                  Option[String] = None,
  phone_number:             Option[String] = None,
  more_number:              Option[String] = None,
  opt_status:               Option[String] = None,
  source_id:                Option[Long]   = None,
  off5th_canada_opt_status: Option[String] = None,
  saks_canada_opt_status:   Option[String] = None
)

trait JodaDateTimeParsing {
  implicit val jodaFooterSignUpDateTimeFormat: Format[DateTime] =
    Format(
      Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ssZ"),
      Writes.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ssZ")
    )
}

object MarketingSignUpModel extends JodaDateTimeParsing {
  implicit val marketingSignUpModelFormat = Json.format[MarketingSignUpModel]
}

case class EmailPreferencesResponseModel(
  enabled:             Boolean,
  email_subscriptions: EmailSubscriptionsModel,
  links:               Option[Map[String, String]],
  messages:            Option[Map[String, String]]
)

case class AccountSettingsResponseModel(
  account_profile:   UserAccountWebsiteModel,
  password_settings: PasswordSettingModel,
  email_preferences: EmailPreferencesResponseModel,
  header:            Seq[Header],
  messages:          Option[Map[String, String]]
)

object PasswordSettingModel {
  implicit val passwordSettingModelFormat = Json.format[PasswordSettingModel]
}

object PreferencesResponseModel {
  implicit val preferencesResponseFormat = Json.format[PreferencesResponseModel]
}

object PreferencesRequestModel {
  implicit val preferencesRequestFormat = Json.format[PreferencesRequestModel]
}

object EmailPreferencesModel {
  val emailPreferencesReads: Reads[EmailPreferencesModel] = (
    (JsPath \ "opt_status").readNullable[String] and
    (JsPath \ "saks_opt_status").readNullable[String] and
    (JsPath \ "off5th_canada_opt_status").readNullable[String] and
    (JsPath \ "saks_canada_opt_status").readNullable[String]
  )(EmailPreferencesModel.apply _)

  val emailPreferencesWrites: Writes[EmailPreferencesModel] = (
    (JsPath \ "opt_status").writeNullable[String] and
    (JsPath \ "saks_opt_status").writeNullable[String] and
    (JsPath \ "off5th_canada_opt_status").writeNullable[String] and
    (JsPath \ "saks_canada_opt_status").writeNullable[String]
  )(unlift(EmailPreferencesModel.unapply))

  implicit val emailPreferencesFormat: Format[EmailPreferencesModel] = Format(emailPreferencesReads, emailPreferencesWrites)
}

object EmailPreferencesResponseModel {
  implicit val emailSubcriptionsModelFormat = Json.format[EmailSubscriptionsModel]
  implicit val emailPreferencesResponseModelFormat = Json.format[EmailPreferencesResponseModel]
}

object AccountSettingsResponseModel {
  implicit val accountSettingsResponseModelFormat = Json.format[AccountSettingsResponseModel]
}
