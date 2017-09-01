package services

import javax.inject.Inject

import constants.Constants._
import helpers.{ AccountHelper, ConfigHelper, TogglesHelper }
import models.ApiErrorModel
import models.servicemodel.{ Order, OrderRequest, Orders, PageInfo }
import models.website._
import monitoring.StatsDClientLike
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Cookie

import scala.concurrent.Future
import scala.util.Success

class OrderService @Inject() (
    httpTransportService: HttpTransportService,
    localizationService:  LocalizationService,
    togglesHelper:        TogglesHelper,
    configHelper:         ConfigHelper,
    accountHelper:        AccountHelper,
    statsDClient:         StatsDClientLike
) {
  val orderServiceUrl: String = configHelper.getStringProp("data-service.order")

  def getOrderHistory(headers: Map[String, String], page_num: Int = 1, pathToResource: String): Future[(OrdersHistoryWebsiteModel, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("OrderService.getOrderHistory")({
      val countriesResponse = localizationService.cachedCountries("countries")
      for {
        res <- httpTransportService.getFromService[Orders](orderServiceUrl, Some("orders"), headers, Map(PAGE_NUM_PARAM -> page_num.toString))
        countries <- countriesResponse
        addFavoritesTab <- togglesHelper.getFavoritesToggleState
        addSaksFirstPage <- togglesHelper.saksFirstPageEnabled
      } yield res match {
        case Left(failureResponse)     => (OrdersHistoryWebsiteModel(false, Seq.empty, accountHelper.getHeader(addFavoritesTab, addSaksFirstPage), PageInfo(0, 0, 0, 0), accountHelper.buildViewMoreOrderLinks(pathToResource), accountHelper.buildOrderMessages, countries), failureResponse.cookies, failureResponse.errors, 200)
        case Right(successfulResponse) => (OrdersHistoryWebsiteModel(true, successfulResponse.body.orders, accountHelper.getHeader(addFavoritesTab, addSaksFirstPage), successfulResponse.body.page_info, accountHelper.buildViewMoreOrderLinks(pathToResource), accountHelper.buildOrderMessages, countries), successfulResponse.cookies, Seq.empty[ApiErrorModel], 200)
      }
    }) andThen {
      case Success(response) =>
        response._1.orders.filter(_.billing_details.billing_address.isEmpty).foreach { order =>
          Logger.warn(s"Missing address on order with order number: ${order.order_num}")
        }
    }
  }

  def getOrderDetails(headers: Map[String, String], payload: OrderRequest, pathToResource: String): Future[(OrderDetailsWebsiteModel, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("OrderService.getOrderDetails")({
      val countriesResponse = localizationService.cachedCountries("countries")
      for {
        res <- httpTransportService.postToService[OrderRequest, Order](orderServiceUrl, Some("orderdetails"), payload, headers)
        countries <- countriesResponse
      } yield res match {
        case Left(failureResponse) =>
          Logger.error(s"Retrieve order details failed with order number: ${payload.order_num} with zip: ${payload.billing_zip_code}")
          (OrderDetailsWebsiteModel(enabled = false, None, PageInfo(0, 0, 0, 0), accountHelper.buildCancelOrderLink(pathToResource), accountHelper.buildOrderMessages, countries), failureResponse.cookies, failureResponse.errors, failureResponse.code)
        case Right(s) => (OrderDetailsWebsiteModel(enabled = true, Some(s.body), PageInfo(1, 1, 1, 1), accountHelper.buildCancelOrderLink(pathToResource), accountHelper.buildOrderMessages, countries), s.cookies, Seq.empty[ApiErrorModel], 200)
      }
    }) andThen {
      case Success(response) =>
        response._1.order.foreach { order =>
          if (order.billing_details.billing_address.isEmpty) {
            Logger.warn(s"Missing address on order with order number: ${order.order_num}")
          }
        }
    }
  }

  def cancelOrder(headers: Map[String, String], payload: OrderRequest, pathToResource: String): Future[(Option[Order], Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("OrderService.cancelOrder")({
      val cancelResponse = httpTransportService.postToService[OrderRequest, Order](orderServiceUrl, Some("order/cancel"), payload, headers)
      cancelResponse map {
        case Left(failureResponse) => (None, failureResponse.cookies, failureResponse.errors, failureResponse.code)
        case Right(s)              => (Some(s.body), s.cookies, Seq.empty[ApiErrorModel], 200)
      }
    })
  }
}
