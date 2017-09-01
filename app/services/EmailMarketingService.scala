package services

import java.util.UUID

import com.google.inject.Inject
import helpers.{ AccountHelper, ConfigHelper }
import models.website._
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, Json }

class EmailMarketingService @Inject() (
    httpTransportService: HttpTransportService,
    configHelper:         ConfigHelper,
    accountHelper:        AccountHelper
) {
  val emailMarketingServiceUrl: String = configHelper.getStringProp("data-service.email-marketing")
  val giltSubscriptionUrl: String = configHelper.getStringProp("data-service.gilt")

  def updateEmailProfile(headers: Map[String, String], userId: String, httpModel: JsObject) = {
    httpTransportService.putToService[JsObject, Seq[MarketingSignUpModel]](emailMarketingServiceUrl, Some(s"profile/$userId/user-info"), httpModel, headers)
  }

  def createEmailPreferences(headers: Map[String, String], createAccountRequest: CreateAccountRequest, ipAddress: String, emailPreferences: (String, String, Option[String], Option[String])) = {
    val emailMarketingSignUpModel = createEmailMarketingSignUpModel(createAccountRequest, ipAddress, emailPreferences)
    val httpModel = Json.obj("item" -> emailMarketingSignUpModel)
    httpTransportService.postToService[JsObject, Boolean](emailMarketingServiceUrl, Some("profile/store-profile"), httpModel, headers)
    if (createAccountRequest.preferences.getOrElse(Seq.empty[String]).contains("gilt_opt_status")) {
      httpTransportService.postToExternalService[GiltEmailSubscriptionModel, JsObject](giltSubscriptionUrl, Some("api-user-registration/user_registrations"), buildGiltRegistrationPayload(createAccountRequest), Map.empty)
    }
  }

  private def createEmailMarketingSignUpModel(createAccountRequest: CreateAccountRequest, ipAddress: String, emailPreferences: (String, String, Option[String], Option[String])): JsObject = {
    Json.obj(
      "first_name" -> createAccountRequest.first_name,
      "last_name" -> createAccountRequest.last_name,
      "email_address" -> createAccountRequest.email,
      "canadian_customer" -> createAccountRequest.canadian_customer,
      "subscribed_date" -> dateNowAsString,
      "saks_opt_status" -> getEmailPreference(createAccountRequest, "saks_opt_status", emailPreferences._1),
      "opt_status" -> getEmailPreference(createAccountRequest, "off5th_opt_status", emailPreferences._2),
      "saks_canada_opt_status" -> getEmailPreference(createAccountRequest, "saks_canada_opt_status", getValueOrFalse(emailPreferences._3)),
      "off5th_canada_opt_status" -> getEmailPreference(createAccountRequest, "off5th_canada_opt_status", getValueOrFalse(emailPreferences._4)),
      "ip_address" -> ipAddress
    )
  }

  private def getEmailPreference(createAccountRequest: CreateAccountRequest, preference: String, oldEmailPreference: String) = {
    createAccountRequest.preferences match {
      case Some(preferences) => if (preferences.contains(preference)) "T" else "F"
      case _                 => oldEmailPreference
    }
  }

  private def dateNowAsString: String = {
    DateTime.now().toString("yyyy-MM-dd'T'HH:mm:ssZ")
  }

  private def getValueOrFalse(field: Option[String]): String = {
    field.getOrElse("F")
  }

  private def buildGiltRegistrationPayload(createAccountRequest: CreateAccountRequest): GiltEmailSubscriptionModel = {
    GiltEmailSubscriptionModel(
      GiltAuthentication(EmailPasswordAuthentication(createAccountRequest.email, UUID.randomUUID().toString)),
      createAccountRequest.first_name,
      createAccountRequest.last_name
    )
  }

}
