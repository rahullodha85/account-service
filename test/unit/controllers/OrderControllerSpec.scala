package unit.controllers

import fixtures.RequestFixtures
import models.servicemodel.{ OrderRequest, PageInfo }
import models.website.OrderDetailsWebsiteModel
import org.mockito.Matchers.any
import org.mockito.Mockito.{ mock => _, _ }
import org.scalatest.{ BeforeAndAfterEach, Matchers, WordSpec }
import play.api.inject._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.OrderService
import utils.TestUtils._

import scala.concurrent.Future
import scala.language.postfixOps

class OrderControllerSpec extends WordSpec
    with Matchers
    with BeforeAndAfterEach
    with RequestFixtures {

  val orderService = mock[OrderService]
  val application = createIsolatedApplication()
    .overrides(bind[OrderService].toInstance(orderService))
    .build()

  override def beforeEach(): Unit = {
    reset(orderService)
  }

  "Order Controller" should {

    "get order details" should {

      "return with a status of " + OK + " if order number and billing zip are valid" in {
        val orderDetailsResponse = Future.successful((OrderDetailsWebsiteModel(true, None, PageInfo(1, 1, 1, 1), None, None, Map.empty), Seq.empty, Seq.empty, 200))
        when(orderService.getOrderDetails(any[Map[String, String]], any[OrderRequest], any[String])).thenReturn(orderDetailsResponse)

        val result = route(application, FakeRequest(GET, versionCtx + "/account-service/orders/1234567?billing_zip_code=12345")).get

        status(result) shouldBe OK
        contentType(result).get == "application/json" shouldBe true
      }

      "return with a status of " + BAD_REQUEST + " due to the absence of a request body" in {
        val result = route(application, FakeRequest(GET, versionCtx + "/account-service/orders/aaa?billing_zip_code=@")).get
        status(result) shouldBe BAD_REQUEST
        contentType(result).get == "application/json" shouldBe true
      }
    }
  }

  "cancel order" should {

    "return " + OK + " if order number and billing zip are valid" in {
      when(orderService.cancelOrder(any[Map[String, String]], any[OrderRequest], any[String])).thenReturn(Future.successful((None, Seq.empty, Seq.empty, 200)))

      val validOrderPayload =
        Json.parse("""{
        "order_num" : "1234567",
        "billing_zip_code" : "12345"
        }""")

      val result = route(application, FakeRequest(PUT, versionCtx + "/account-service/orders/1234567").withJsonBody(validOrderPayload)).get
      status(result) shouldBe OK
      contentType(result).get == "application/json" shouldBe true
    }

    "return " + BAD_REQUEST + " given invalid payload" in {
      val result = route(application, FakeRequest(PUT, versionCtx + "/account-service/orders/123456").withJsonBody(Json.obj())).get
      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "errors" \\ "data").head.as[String] shouldBe "Please enter a valid order number."
      (contentAsJson(result) \ "errors" \\ "error").head.as[String] shouldBe "order_num"
      (contentAsJson(result) \ "errors" \\ "data")(1).as[String] shouldBe "Please enter a valid billing zip code."
      (contentAsJson(result) \ "errors" \\ "error")(1).as[String] shouldBe "billing_zip_code"
    }

    "return " + BAD_REQUEST + " if oms reservation is not alpanumeric" in {

      val invalidOrderPayload =
        Json.parse("""{
        "order_num" : "1234567",
        "billing_zip_code" : "12345",
        "oms_reservation_id": "@"
        }""")

      val result = route(application, FakeRequest(PUT, versionCtx + "/account-service/orders/1234567").withJsonBody(invalidOrderPayload)).get
      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "errors" \\ "data").head.as[String] shouldBe "Please enter a valid OMS reservation id."
      (contentAsJson(result) \ "errors" \\ "error").head.as[String] shouldBe "oms_reservation_id"
    }
  }
}
