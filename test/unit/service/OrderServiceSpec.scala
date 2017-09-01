package unit.service

import constants.Constants._
import helpers.{ AccountHelper, ConfigHelper, TogglesHelper }
import models.servicemodel._
import models.{ ApiErrorModel, FailureResponse, SuccessfulResponse }
import org.mockito.Matchers.{ eq => matchEqual }
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }
import play.api.mvc.Cookie
import services.{ HttpTransportService, OrderService }
import utils.NoOpStatsDClient
import utils.TestUtils._

import scala.concurrent.Future
import scala.util.Right

class OrderServiceSpec extends WordSpec
    with MockitoSugar
    with Matchers {

  val headers: Map[String, String] = Map("JSESSIONID" -> "123")
  val accountHelper = createIsolatedApplication().build().injector.instanceOf[AccountHelper]

  "Order Service" should {
    "Get order history" should {
      "return order history with status code 200 when httpTransport returns Right response" in {
        val togglesHelper = mock[TogglesHelper]
        when(togglesHelper.getFavoritesToggleState).thenReturn(Future.successful(false))
        when(togglesHelper.saksFirstPageEnabled).thenReturn(Future.successful(false))

        val httpTransportService = mock[HttpTransportService]
        val page_num = 5
        val ordersResponse: Orders = Orders(Seq.empty[Order], PageInfo(0, 0, 0, 0))
        when(httpTransportService.getFromService[Orders](ConfigHelper.getStringProp("data-service.order"), Some("orders"), headers, Map(PAGE_NUM_PARAM -> page_num.toString))).thenReturn(Future.successful(Right(SuccessfulResponse(ordersResponse, Seq.empty[Cookie]))))
        val orderService = new OrderService(httpTransportService, localizationService, togglesHelper, ConfigHelper, accountHelper, NoOpStatsDClient)

        whenReady(orderService.getOrderHistory(headers, page_num, "some/path")) { response =>
          response._1.enabled should be(true)
          response._1.orders should be(ordersResponse.orders)
          response._2.isEmpty should be(true)
          response._3.isEmpty should be(true)
          response._4 should be(200)
        }
      }

      "return errors with status code 200 when httpTransport returns Left response" in {
        val togglesHelper = mock[TogglesHelper]
        when(togglesHelper.getFavoritesToggleState).thenReturn(Future.successful(false))
        when(togglesHelper.saksFirstPageEnabled).thenReturn(Future.successful(false))

        val httpTransportService = mock[HttpTransportService]
        val page_num = 5
        val expectedErrorResponse = Future.successful(Left(FailureResponse(Seq(ApiErrorModel("Error here", "error.error")), 500)))
        when(httpTransportService.getFromService[Orders](ConfigHelper.getStringProp("data-service.order"), Some("orders"), headers, Map(PAGE_NUM_PARAM -> page_num.toString))).thenReturn(expectedErrorResponse)
        val orderService = new OrderService(httpTransportService, localizationService, togglesHelper, ConfigHelper, accountHelper, NoOpStatsDClient)

        whenReady(orderService.getOrderHistory(headers, page_num, "some/path")) { response =>
          response._1.enabled should be(false)
          response._3.length should be(1)
          response._4 should be(200)
        }
      }
    }

    "Get order details" should {
      "return order details with status code 200 when httpTransport returns Right response" in {
        val ordersResponse = mock[Order]
        val httpTransportService = mock[HttpTransportService]

        val request: OrderRequest = OrderRequest("1234", "123")
        when(httpTransportService.postToService[OrderRequest, Order](ConfigHelper.getStringProp("data-service.order"), Some("orderdetails"), request, headers)).thenReturn(Future.successful(Right(SuccessfulResponse(ordersResponse, Seq.empty[Cookie]))))
        val orderService = new OrderService(httpTransportService, localizationService, mock[TogglesHelper], ConfigHelper, accountHelper, NoOpStatsDClient)

        whenReady(orderService.getOrderDetails(headers, request, "some/path")) { response =>
          response._1.enabled should be(true)
          response._1.order.get should be(ordersResponse)
          response._2.isEmpty should be(true)
          response._3.isEmpty should be(true)
          response._4 should be(200)
        }
      }

      "return errors with status code 500 when httpTransport returns Left response" in {
        val expectedErrorResponse: Future[Either[FailureResponse, SuccessfulResponse[Order]]] = Future.successful(Left(FailureResponse(Seq(ApiErrorModel("Error here", "error.error")), 500)))
        val request: OrderRequest = OrderRequest("1234", "123")
        val httpTransportService = mock[HttpTransportService]

        when(httpTransportService.postToService[OrderRequest, Order](ConfigHelper.getStringProp("data-service.order"), Some("orderdetails"), request, headers)).thenReturn(expectedErrorResponse)

        val orderService = new OrderService(httpTransportService, localizationService, mock[TogglesHelper], ConfigHelper, accountHelper, NoOpStatsDClient)

        whenReady(orderService.getOrderDetails(headers, request, "some/path")) { response =>
          response._1.enabled should be(false)
          response._3.length should be(1)
          response._4 should be(500)
        }
      }
    }

    "Cancel order" should {
      "return cancelled order with status code 200 when httpTransport returns Right response" in {
        val order = mock[Order]
        val cancelRequest = OrderRequest("100292921", "10018")
        val httpTransportService = mock[HttpTransportService]

        when(httpTransportService.postToService[OrderRequest, Order](ConfigHelper.getStringProp("data-service.order"), Some("order/cancel"), cancelRequest, headers)).thenReturn(Future.successful(Right(SuccessfulResponse(order, Seq.empty[Cookie]))))

        val orderService = new OrderService(httpTransportService, localizationService, mock[TogglesHelper], ConfigHelper, accountHelper, NoOpStatsDClient)

        whenReady(orderService.cancelOrder(headers, cancelRequest, "some/path")) { response =>
          response._1 should be(Some(order))
          response._2.isEmpty should be(true)
          response._3.isEmpty should be(true)
          response._4 should be(200)
        }
      }

      "return errors with status code 500 when httpTransport returns Left response" in {
        val expectedErrorResponse: Future[Left[FailureResponse, Nothing]] = Future.successful(Left(FailureResponse(Seq(ApiErrorModel("Error here", "error.error")), 500)))
        val cancelRequest = OrderRequest("100292921", "10018")
        val httpTransportService = mock[HttpTransportService]

        when(httpTransportService.postToService[OrderRequest, Order](ConfigHelper.getStringProp("data-service.order"), Some("order/cancel"), cancelRequest, headers)).thenReturn(expectedErrorResponse)

        val orderService = new OrderService(httpTransportService, localizationService, mock[TogglesHelper], ConfigHelper, accountHelper, NoOpStatsDClient)

        whenReady(orderService.cancelOrder(headers, cancelRequest, "some/path")) { response =>
          response._1 should be(None)
          response._3.length should be(1)
          response._4 should be(500)
        }
      }
    }
  }
}
