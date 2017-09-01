package services

import javax.inject.Inject

import constants.Banners
import constants.Constants._
import helpers.{ AccountHelper, ConfigHelper, TogglesHelper }
import models.servicemodel.PaymentMethodsModel
import models.website._
import models.{ ApiErrorModel, FailureResponse, SuccessfulResponse }
import monitoring.StatsDClientLike
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Cookie

import scala.concurrent.Future

class PaymentMethodService @Inject() (
    httpTransportService: HttpTransportService,
    accountHelper:        AccountHelper,
    statsDClient:         StatsDClientLike,
    togglesHelper:        TogglesHelper,
    configHelper:         ConfigHelper,
    rewardsService:       RewardsService
) {

  val paymentMethodServiceUrl: String = configHelper.getStringProp("data-service.payment-method")

  def getPaymentMethod(headers: Map[String, String], pathToResource: String) = {
    statsDClient.time("PaymentMethodService.getPaymentMethod") {
      for {
        paymentMethodResponse <- httpTransportService.getFromService[PaymentMethodsModel](paymentMethodServiceUrl, Some("payment-method"), headers)
        addFavoritesTab <- togglesHelper.getFavoritesToggleState
        addSaksFirstPage <- togglesHelper.saksFirstPageEnabled
      } yield {
        paymentMethodResponse match {
          case Left(failureResponse) =>
            val paymentTabResponse = PaymentTabResponseWebsiteModel(enabled = false, Seq.empty, accountHelper.addAnotherPayment, accountHelper.getHeader(addFavoritesTab, addSaksFirstPage), accountHelper.buildPaymentMethodLinks(pathToResource), accountHelper.buildPaymentMethodMessages, accountHelper.getMonths)
            (paymentTabResponse, failureResponse.cookies, failureResponse.errors, 200)
          case Right(successfulResponse) =>
            val paymentTabResponse = PaymentTabResponseWebsiteModel(enabled = true, accountHelper.createPaymentMethodInfoModel(successfulResponse.body.credit_cards), accountHelper.addAnotherPayment, accountHelper.getHeader(addFavoritesTab, addSaksFirstPage), accountHelper.buildPaymentMethodLinks(pathToResource), accountHelper.buildPaymentMethodMessages, accountHelper.getMonths)
            (paymentTabResponse, successfulResponse.cookies, Seq.empty[ApiErrorModel], 200)
        }
      }
    }
  }

  def deletePaymentMethod(headers: Map[String, String], id: String) =
    statsDClient.time("PaymentMethodService.deletePaymentMethod")({
      process(httpTransportService.deleteFromService[PaymentMethodsModel](paymentMethodServiceUrl, Some(s"payment-method/$id"), headers))
    })

  def createPaymentMethod(headers: Map[String, String], createPaymentMethodRequest: CreatePaymentMethodRequest, accountId: String) = {
    statsDClient.time("PaymentMethodService.createPaymentMethod") {
      process(httpTransportService.postToService[CreatePaymentMethodRequest, PaymentMethodsModel](paymentMethodServiceUrl, Some("payment-method"), createPaymentMethodRequest, headers)).flatMap { paymentResponse =>
        if (shouldLink(createPaymentMethodRequest, paymentResponse)) {
          rewardsService.linkSaksFirstAccount(headers, LinkSaksFirstRequest(createPaymentMethodRequest.name, createPaymentMethodRequest.zip.get, createPaymentMethodRequest.number), accountId)
        }
        Future.successful(paymentResponse)
      }
    }
  }

  def updatePaymentMethod(headers: Map[String, String], paymentId: String, updatePaymentMethodRequest: UpdatePaymentMethodRequest) =
    statsDClient.time("PaymentMethodService.updatePaymentMethod")({
      process(httpTransportService.putToService[UpdatePaymentMethodRequest, PaymentMethodsModel](paymentMethodServiceUrl, Some(s"payment-method/$paymentId"), updatePaymentMethodRequest, headers))
    })

  private def process(response: Future[Either[FailureResponse, SuccessfulResponse[PaymentMethodsModel]]]): Future[(PaymentTabPostResponseWebsiteModel, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    response map {
      case Left(failureResponse)     => (PaymentTabPostResponseWebsiteModel(Seq.empty), failureResponse.cookies, failureResponse.errors, failureResponse.code)
      case Right(successfulResponse) => (PaymentTabPostResponseWebsiteModel(accountHelper.createPaymentMethodInfoModel(successfulResponse.body.credit_cards)), successfulResponse.cookies, Seq.empty[ApiErrorModel], 200)
    }
  }
  private def shouldLink(createPaymentMethodRequest: CreatePaymentMethodRequest, paymentResponse: (PaymentTabPostResponseWebsiteModel, Seq[Cookie], Seq[ApiErrorModel], Int)) = {
    isSaksBanner && paymentResponse._3.isEmpty && isSaksCardRequestWithZipCode(createPaymentMethodRequest) && !hasSaksCard(paymentResponse._1, createPaymentMethodRequest.number)
  }

  private def hasSaksCard(response: PaymentTabPostResponseWebsiteModel, cardNumber: String): Boolean = {
    response.payment_methods_info.tail.exists { card =>
      card.credit_card.brand.equals(PaymentMethodType.SAKS.toString)
    }
  }

  private def isSaksBanner: Boolean = configHelper.banner match {
    case Banners.Saks => true
    case _            => false
  }

  private def isSaksCardRequestWithZipCode(createPaymentMethodRequest: CreatePaymentMethodRequest): Boolean = {
    createPaymentMethodRequest.brand.equals(PaymentMethodType.SAKS.toString) && createPaymentMethodRequest.zip.isDefined
  }

}
