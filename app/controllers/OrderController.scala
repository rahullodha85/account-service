package controllers

import javax.inject.Inject

import constants.Constants._
import helpers._
import models.servicemodel.OrderRequest
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc._
import services.OrderService
import validations.Validator

import scala.concurrent.Future

class OrderController @Inject() (
    orderService: OrderService,
    validator:    Validator
) extends Controller with ControllerPayload with RequestHelper {

  def getOrderHistory(account_id: String, page_num: Int = 1) = Authorized { (headers, request) =>
    implicit val req = request
    orderService.getOrderHistory(headers, page_num, request.path).map { e => writeResponse(e) }
  }

  def getOrderDetails(order_id: String, billing_zip_code: String) = Unauthenticated { (headers, request) =>
    implicit val req = request
    val getOrderDetailsBody = Json.obj(
      "order_num" -> order_id,
      "billing_zip_code" -> billing_zip_code
    ).asOpt[JsValue]
    validator.validate[OrderRequest](getOrderDetailsBody) match {
      case Right(orderDetailsRequest) =>
        orderService.getOrderDetails(headers, orderDetailsRequest, request.path).map { e => writeResponse(e) }
      case Left(errors) =>
        Future.successful(writeResponseError(errors, Status(400)))
    }
  }

  def cancelOrder(order_id: String) = Unauthenticated { (headers, request) =>
    implicit val req = request
    validator.validate[OrderRequest](request.body.asJson) match {
      case Right(cancelOrderRequest) =>
        val additionalHeaders = Map(API_KEY -> API_KEY_VAL)
        orderService.cancelOrder(headers ++ additionalHeaders, cancelOrderRequest, request.path).map { e => writeResponse(e) }
      case Left(errors) =>
        Future.successful(writeResponseError(errors, Status(400)))
    }
  }
}
