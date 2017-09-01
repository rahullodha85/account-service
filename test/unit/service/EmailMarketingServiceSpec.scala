package unit.service

import builders.requests.CreateAccountPayloadBuilder
import helpers.{ AccountHelper, ConfigHelper, TogglesHelper }
import models.website.{ CreateAccountRequest, GiltEmailSubscriptionModel }
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{ any, eq => eql }
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }
import play.api.libs.json.JsObject
import services.{ EmailMarketingService, HttpTransportService }
import utils.TestUtils.createIsolatedApplication
import validations.Validator

class EmailMarketingServiceSpec extends WordSpec
    with Matchers
    with MockitoSugar {

  private val injector = createIsolatedApplication().build().injector
  val accountHelper = injector.instanceOf[AccountHelper]
  val validator = injector.instanceOf[Validator]
  val headers: Map[String, String] = Map("JSESSIONID" -> "123")
  val fakeTogglesHelper: TogglesHelper = mock[TogglesHelper]

  "Email Marketing Service" should {
    "Create Email Preferences Action" should {

      "return saks opt saks true when preferences are set" in {
        val httpTransport = mock[HttpTransportService]
        val configHelper = mock[ConfigHelper]
        val accountHelper = mock[AccountHelper]

        val emailMarketingService = new EmailMarketingService(httpTransport, configHelper, accountHelper)

        val captor = ArgumentCaptor.forClass(classOf[JsObject])
        val emailPreferences = ("F", "F", None, None)

        emailMarketingService.createEmailPreferences(headers, new CreateAccountPayloadBuilder().withPreferences(Seq("saks_opt_status")).build(), "0.0.0.0", emailPreferences)
        verify(httpTransport).postToService[JsObject, Boolean](any(), eql(Some("profile/store-profile")), captor.capture(), eql(headers), any())(any(), any())
        val item: JsObject = (captor.getValue \ "item").as[JsObject]
        (item \ "saks_opt_status").as[String] should be("T")
        (item \ "opt_status").as[String] should be("F")
      }

      "create gilt profile when opting in to gilt emails" in {
        val httpTransport = mock[HttpTransportService]
        val configHelper = mock[ConfigHelper]
        val accountHelper = mock[AccountHelper]

        val emailMarketingService = new EmailMarketingService(httpTransport, configHelper, accountHelper)

        val captorGilt = ArgumentCaptor.forClass(classOf[GiltEmailSubscriptionModel])
        val emailPreferences = ("F", "F", None, None)

        emailMarketingService.createEmailPreferences(headers, new CreateAccountPayloadBuilder().withFirstName("jack").withPreferences(Seq("saks_opt_status", "gilt_opt_status")).build(), "0.0.0.0", emailPreferences)
        verify(httpTransport).postToExternalService[GiltEmailSubscriptionModel, JsObject](any(), eql(Some("api-user-registration/user_registrations")), captorGilt.capture(), eql(Map.empty), any())(any(), any())
        val giltModel: GiltEmailSubscriptionModel = captorGilt.getValue
        giltModel.first_name should be("jack")
      }

      "use old model when no preferences are set" in {
        val httpTransport = mock[HttpTransportService]
        val configHelper = mock[ConfigHelper]
        val accountHelper = mock[AccountHelper]

        val emailMarketingService = new EmailMarketingService(httpTransport, configHelper, accountHelper)

        val captor = ArgumentCaptor.forClass(classOf[JsObject])
        val emailPreferences = ("F", "T", None, None)
        val request = CreateAccountRequest("jack", "johnson", "123", "123", "jack@email.test", Some("F"), Some(false), Some("F"), Some("F"), None, None, None)

        emailMarketingService.createEmailPreferences(headers, request, "0.0.0.0", emailPreferences)
        verify(httpTransport).postToService[JsObject, Boolean](any(), eql(Some("profile/store-profile")), captor.capture(), eql(headers), any())(any(), any())
        val item: JsObject = (captor.getValue \ "item").as[JsObject]
        (item \ "saks_opt_status").as[String] should be("F")
        (item \ "opt_status").as[String] should be("T")
      }
    }
  }
}