package models.website

import constants.Constants._
import models.servicemodel._
import play.api.libs.json.Json

import scala.util.Try

case class Month(options: Seq[ValueLabel])
case class ValueLabel(value: Int, label: String)

case class PaymentTabResponseWebsiteModel(enabled: Boolean, payment_methods_info: Seq[PaymentMethodInfoModel], add_another: AddAnotherWebsiteModel, header: Seq[Header], links: Option[Map[String, String]], messages: Option[Map[String, String]], months: Month) extends ResponseModel

case class PaymentTabPostResponseWebsiteModel(
  payment_methods_info: Seq[PaymentMethodInfoModel]
)

object PaymentTabPostResponseWebsiteModel extends AddAnotherWebsiteModelFormats with PaymentMethodSupport {
  implicit val paymentMethodInfoModelFormat = Json.format[PaymentMethodInfoModel]
  implicit val paymentMethodsInfoModelFormat = Json.format[PaymentMethodsInfoModel]
  implicit val paymentTabPostResponseWebsiteModelFormat = Json.format[PaymentTabPostResponseWebsiteModel]
}

object PaymentTabResponseWebsiteModel extends AddAnotherWebsiteModelFormats with PaymentMethodSupport {
  implicit val paymentMethodInfoModelFormat = Json.format[PaymentMethodInfoModel]
  implicit val paymentMethodsInfoModelFormat = Json.format[PaymentMethodsInfoModel]
  implicit val valueLabelFormat = Json.format[ValueLabel]
  implicit val monthFormat = Json.format[Month]
  implicit val paymentTabResponseWebsiteModel = Json.format[PaymentTabResponseWebsiteModel]
}

case class PaymentMethodInfoModel(
  credit_card:        PaymentMethodModel,
  allowed_operation:  Map[String, Boolean],
  display_brand_name: String
)

case class PaymentMethodsInfoModel(
  credit_cards: Seq[PaymentMethodInfoModel]
)

trait PaymentMethodSupport {
  val saks_store_card = "Saks Store Card"
  val saks_master_card = "Saks MasterCard"
  val mastercard = "MasterCard"
  val visa = "Visa"
  val amex = "American Express"
  val discover = "Discover"
  val japan_credit_burreau = "Japan Credit Bureau"
  val china_union_pay_credit = "China UnionPay Credit"
  val dinersclub = "Diners Club"
  val paypal = "PayPal"

  def edit_delete = Map(ALLOWED_OPRERATION_EDIT_KEY -> true, ALLOWED_OPRERATION_DELETE_KEY -> true)
  def delete = Map(ALLOWED_OPRERATION_EDIT_KEY -> false, ALLOWED_OPRERATION_DELETE_KEY -> true)

}

object PaymentMethodSupport extends PaymentMethodSupport
object PaymentMethodType extends Enumeration {
  val AMEX, DINERS, MC, DISC, VISA, JCB, CUP, SAKS, SMC, PAYPAL = Value

  def isValidBrandName(brandName: String): Boolean = {
    Try {
      PaymentMethodType.withName(brandName)
    }.isSuccess
  }

}
sealed abstract class SupportedPaymentMethods(val paymentMethodType: PaymentMethodType.Value, val display_name: String, val allowed_operation: Map[String, Boolean])
case object SAKS extends SupportedPaymentMethods(PaymentMethodType.SAKS, PaymentMethodSupport.saks_store_card, PaymentMethodSupport.edit_delete)
case object SMC extends SupportedPaymentMethods(PaymentMethodType.SMC, PaymentMethodSupport.saks_master_card, PaymentMethodSupport.edit_delete)
case object MC extends SupportedPaymentMethods(PaymentMethodType.MC, PaymentMethodSupport.mastercard, PaymentMethodSupport.edit_delete)
case object VISA extends SupportedPaymentMethods(PaymentMethodType.VISA, PaymentMethodSupport.visa, PaymentMethodSupport.edit_delete)
case object AMEX extends SupportedPaymentMethods(PaymentMethodType.AMEX, PaymentMethodSupport.amex, PaymentMethodSupport.edit_delete)
case object DISC extends SupportedPaymentMethods(PaymentMethodType.DISC, PaymentMethodSupport.discover, PaymentMethodSupport.edit_delete)
case object JCB extends SupportedPaymentMethods(PaymentMethodType.JCB, PaymentMethodSupport.japan_credit_burreau, PaymentMethodSupport.edit_delete)
case object CUP extends SupportedPaymentMethods(PaymentMethodType.CUP, PaymentMethodSupport.china_union_pay_credit, PaymentMethodSupport.edit_delete)
case object DINERS extends SupportedPaymentMethods(PaymentMethodType.DINERS, PaymentMethodSupport.dinersclub, PaymentMethodSupport.edit_delete)
case object PAYPAL extends SupportedPaymentMethods(PaymentMethodType.PAYPAL, PaymentMethodSupport.paypal, PaymentMethodSupport.delete)

object PaymentMethodInfoModel extends PaymentMethodSupport {

  implicit val paymentMethodInfoModel = Json.format[PaymentMethodInfoModel]

  def populateAllowedOperation(payment: PaymentMethodModel): PaymentMethodInfoModel = {

    PaymentMethodType.withName(payment.brand) match {
      case AMEX.paymentMethodType   => PaymentMethodInfoModel(payment, AMEX.allowed_operation, AMEX.display_name)
      case DINERS.paymentMethodType => PaymentMethodInfoModel(payment, DINERS.allowed_operation, DINERS.display_name)
      case MC.paymentMethodType     => PaymentMethodInfoModel(payment, MC.allowed_operation, MC.display_name)
      case DISC.paymentMethodType   => PaymentMethodInfoModel(payment, DISC.allowed_operation, DISC.display_name)
      case VISA.paymentMethodType   => PaymentMethodInfoModel(payment, VISA.allowed_operation, VISA.display_name)
      case JCB.paymentMethodType    => PaymentMethodInfoModel(payment, JCB.allowed_operation, JCB.display_name)
      case CUP.paymentMethodType    => PaymentMethodInfoModel(payment, CUP.allowed_operation, CUP.display_name)
      case SAKS.paymentMethodType   => PaymentMethodInfoModel(payment, SAKS.allowed_operation, SAKS.display_name)
      case SMC.paymentMethodType    => PaymentMethodInfoModel(payment, SMC.allowed_operation, SMC.display_name)
      case PAYPAL.paymentMethodType => PaymentMethodInfoModel(payment, PAYPAL.allowed_operation, PAYPAL.display_name)
    }

  }
}
