package controllers

import javax.inject.Inject

import helpers.{ ControllerPayload, RequestHelper }
import models.website.{ CreatePaymentMethodRequest, UpdatePaymentMethodRequest }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import services.PaymentMethodService
import validations.Validator

import scala.concurrent.Future

class PaymentMethodController @Inject() (
    paymentMethodService: PaymentMethodService,
    validator:            Validator
) extends Controller with ControllerPayload with RequestHelper {

  def getPaymentMethod(account_id: String) = Authorized { (headers, request) =>
    implicit val req = request
    paymentMethodService.getPaymentMethod(headers, request.path).map { e =>
      writeResponse(e)
    }
  }

  def deletePaymentMethod(account_id: String, payment_method_id: String) = Authorized { (headers, request) =>
    implicit val req = request
    paymentMethodService.deletePaymentMethod(headers, payment_method_id).map { e => writeResponse(e) }
  }

  def updatePaymentMethod(account_id: String, payment_method_id: String) = Authorized { (headers, request) =>
    implicit val req = request
    validator.validate[UpdatePaymentMethodRequest](request.body.asJson) match {
      case Right(paymentMethodRequest) =>
        val validateJsonErrors = validator.validate(ruleSet = validator.paymentMethodValidation, Json.toJson(paymentMethodRequest))
        if (validateJsonErrors.isEmpty) {
          paymentMethodService.updatePaymentMethod(headers, paymentMethodRequest.id.toString, paymentMethodRequest).map { e => writeResponse(e) }
        } else {
          Future.successful(writeResponseError(validateJsonErrors, Status(400)))
        }
      case Left(errors) =>
        Future.successful(writeResponseError(errors, Status(400)))
    }
  }

  def createPaymentMethod(account_id: String) = Authorized { (headers, request) =>
    implicit val req = request
    validator.validate[CreatePaymentMethodRequest](request.body.asJson) match {
      case Right(paymentMethodRequest) =>
        val validateJsonErrors = validator.validate(ruleSet = validator.paymentMethodValidation, Json.toJson(paymentMethodRequest))
        if (validateJsonErrors.isEmpty) {
          paymentMethodService.createPaymentMethod(headers, paymentMethodRequest, account_id).map { e => writeResponse(e) }
        } else {
          Future.successful(writeResponseError(validateJsonErrors, Status(400)))
        }
      case Left(errors) =>
        Future.successful(writeResponseError(errors, Status(400)))
    }
  }
}
