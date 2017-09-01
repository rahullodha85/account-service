package integration.controllers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import play.api.mvc.Results
import play.api.test.Helpers._
import play.api.test.{ FakeHeaders, FakeRequest }
import utils.TestUtils._

class PaymentControllerValidationSpec extends PlaySpec
    with Results
    with BeforeAndAfterAll
    with MockitoSugar {

  var application = createIsolatedApplication().build()

  "Payment Controller" should {

    "throw 400 for if you fail to input an id" in {
      val requestBody = JsObject(Seq(
        ("brand", JsString("VISA")),
        ("is_default", JsBoolean(true)),
        ("month", JsNumber(5)),
        ("name", JsString("my card")),
        ("number", JsString("4111111111111111")),
        ("security_code", JsString("")),
        ("user_id", JsString("abc@test.com")),
        ("year", JsNumber(2020)),
        ("id", JsNull)
      ))

      val request = FakeRequest(PUT, versionCtx + "/account-service/accounts/123/payment-methods/567", FakeHeaders(data = Seq(("Cookie", "UserName=user@user.com;JSESSIONID=hshsy263"))), body = requestBody)
      val createPaymentResponse = route(application, request).get

      status(createPaymentResponse) mustEqual BAD_REQUEST
      contentType(createPaymentResponse).get == "application/json" mustEqual true
      (contentAsJson(createPaymentResponse) \ "response" \ "results").asOpt[String] mustEqual None
      val errors: Seq[JsObject] = (contentAsJson(createPaymentResponse) \ "errors").as[Seq[JsObject]]
      errors.size mustEqual 1
      (errors.head \ "data").as[String] mustEqual "This field should be a number"
      (errors.head \ "error").as[String] mustEqual "id"
    }
  }
}
