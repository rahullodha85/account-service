package models.servicemodel

import models.FieldConstraints
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.Constants._

case class OrderRequest(
  order_num:          String,
  billing_zip_code:   String,
  oms_reservation_id: Option[String] = None
)

case class Status(
  item:                String,
  inventory:           String,
  auth:                String,
  reason:              String,
  item_status_message: Option[String]
)

case class ItemShippingDetails(
  extended_shipping_charge:         Int,
  display_extended_shipping_charge: String,
  estimated_delivery_date:          String,
  signature_required:               Boolean,
  shipping_method:                  Option[String],
  extended_shipping_tax:            Int,
  display_extended_shipping_tax:    String,
  tracking_numbers:                 Seq[String],
  tracking_links:                   Seq[String]
)

case class ProductInfo(
  product_code:            String,
  upc_code:                String,
  brand_name:              String,
  short_description:       String,
  size:                    String,
  color:                   String,
  product_details_url:     Option[String],
  product_image_url:       Option[String],
  product_review_url:      Option[String],
  is_electronic_gift_card: Boolean,
  is_gift_with_purchase:   Boolean,
  is_virtual:              Boolean,
  is_reviewable:           Boolean,
  is_ship_from_store:      Boolean,
  is_returnable:           Boolean
)

case class Products(
  product: Seq[ProductInfo]
)

case class PersonalizationInfo(
  color:   String,
  text:    String,
  style:   String,
  process: String
)

case class VirtualDeliveryDetails(
  recipient_name:  String,
  recipient_email: String,
  sender_name:     String,
  delivery_date:   String,
  message:         String
)

case class Item(
  status:                         Status,
  product_info:                   Option[ProductInfo],
  extended_price:                 Int,
  back_ordered:                   Boolean,
  drop_ship:                      Boolean,
  pre_order:                      Boolean,
  quantity:                       Int,
  unit_price:                     Int,
  extended_merchandise_tax:       Int,
  unit_offer_price:               Int,
  extended_promo_amount:          Int,
  unit_msrp:                      Int,
  customer_display_unit_price:    Int,
  promo_codes:                    Seq[String],
  unit_associate_discount_amount: Int,
  inventory_source:               String,
  display_prices:                 DisplayItemPrice,
  shipping_details:               Option[ItemShippingDetails],
  personalization_details:        Option[PersonalizationInfo],
  virtual_delivery_details:       Option[VirtualDeliveryDetails],
  gift_idx:                       Option[Int]
)

case class DisplayItemPrice(
  display_extended_price:                 String,
  display_unit_price:                     String,
  display_extended_merchandise_tax:       String,
  display_unit_offer_price:               String,
  display_extended_promo_amount:          String,
  display_unit_msrp:                      String,
  display_customer_display_unit_price:    String,
  display_unit_associate_discount_amount: String
)

case class ItemResponse(
  items:                    Seq[Item],
  shipping_addresses:       Option[CustomerAddress],
  is_virtual_group:         Boolean                    = false,
  display_promo_code:       Option[Boolean],
  is_pickup_in_store_group: Option[Boolean]            = Some(false),
  pickup_in_store_details:  Option[PickupStoreDetails]
)

case class PickupStoreDetails(
  store:            Store,
  primary_person:   Person,
  alternate_person: Person
)

case class Store(
  store_name:      String,
  store_details:   StoreAddress,
  store_info_link: String
)

case class StoreAddress(
  address_1:   String,
  address_2:   Option[String],
  city:        String,
  state:       String,
  postal_code: String,
  phone:       String
)

case class Person(
  first_name: Option[String],
  last_name:  Option[String],
  email:      Option[String],
  phone:      Option[String]
)

case class Order(
  order_creation_date: String,
  order_num:           String,
  ref_num:             String,
  oms_reservation_id:  String,
  status:              String,
  total_items:         Int,
  is_international:    Option[Boolean],
  customer_info:       CustomerInfo,
  billing_details:     BillingDetails,
  all_items:           Seq[ItemResponse],
  giftings:            Option[Seq[Giftings]]
)

case class PageInfo(
  current_page:  Int,
  page_size:     Int,
  total_items:   Int,
  current_items: Int
)

case class Orders(
  orders:    Seq[Order],
  page_info: PageInfo
)

case class BillingDetails(
  payment_method:  PaymentMethod,
  billing_address: Option[CustomerAddress],
  order_summary:   OrderSummary
)

case class CustomerAddress(
  country:     String,
  last_name:   String,
  state:       String,
  address1:    String,
  address2:    String,
  title:       String,
  city:        String,
  phone:       String,
  first_name:  String,
  zip:         String,
  middle_name: String,
  company:     String
)

case class CustomerInfo(
  email:               String,
  loyalties:           Seq[Loyalties] = Seq.empty,
  registered_customer: Boolean,
  saks_employee:       Boolean
)

case class Loyalties(
  id:   String,
  name: String
)

case class PaymentMethod(
  credit_card: Option[CreditCard],
  paypal:      Option[Paypal],
  gift_cards:  Seq[GiftCards]     = Seq.empty
)

case class Paypal(
  email_address:  String,
  amount:         Int,
  display_amount: String
)

case class GiftCards(
  gift_card_number: String,
  pin:              String,
  amount:           Int,
  display_amount:   String
)

case class CreditCard(
  name:           String,
  brand:          String,
  number:         String,
  month:          Int,
  year:           Int,
  security_code:  String,
  display_amount: String
)

case class OrderSummary(
  tax:                      Int,
  original_items_total:     Int,
  total_before_tax:         Int,
  gift_card_total:          Int,
  order_discount:           Int,
  credit_card_total:        Int,
  shipping_total:           Int,
  grand_total:              Int,
  items_total:              Int,
  gift_wrap_total:          Option[Int],
  associate_discount_total: Int,
  msrp_total:               Int,
  you_save:                 Int,
  display_prices:           DisplaySummaryPrices
)

case class DisplaySummaryPrices(
  display_tax:                      String,
  display_original_items_total:     String,
  display_total_before_tax:         String,
  display_gift_card_total:          String,
  display_order_discount:           String,
  display_credit_card_total:        String,
  display_shipping_total:           String,
  display_grand_total:              String,
  display_items_total:              String,
  display_gift_wrap_total:          Option[String],
  display_associate_discount_total: String,
  display_msrp_total:               String,
  display_you_save:                 String
)

case class Giftings(
  gift_wrap_type:     Option[String],
  gift_message_lines: Option[Seq[LineText]]
)

case class LineText(
  line_text: String
)

object ItemShippingDetails {
  implicit val itemShippingDetailsReads: Reads[ItemShippingDetails] = (
    (__ \ "extended_shipping_charge").read[Int] and
    (__ \ "display_extended_shipping_charge").read[String] and
    (__ \ "estimated_delivery_date").read[String].map(_.trim) and
    (__ \ "signature_required").read[Boolean] and
    (__ \ "shipping_method").readNullable[String] and
    (__ \ "extended_shipping_tax").read[Int] and
    (__ \ "display_extended_shipping_tax").read[String] and
    (__ \ "tracking_numbers").read[Seq[String]] and
    (__ \ "tracking_links").read[Seq[String]]
  )(ItemShippingDetails.apply _)

  implicit val itemShippingDetailsWrites = Json.writes[ItemShippingDetails]
}

object Order {
  implicit val statusFormat = Json.format[Status]
  implicit val productInfoFormat = Json.format[ProductInfo]
  implicit val productsFormat = Json.format[Products]
  implicit val personalizationInfo = Json.format[PersonalizationInfo]
  implicit val virtualDeliveryDetails = Json.format[VirtualDeliveryDetails]
  implicit val displayItemPrice = Json.format[DisplayItemPrice]
  implicit val itemFormat = Json.format[Item]
  implicit val creditCardInfoFormat = Json.format[CreditCard]
  implicit val giftCardFormat = Json.format[GiftCards]
  implicit val paypalFormat = Json.format[Paypal]
  implicit val paymentMethodAddressFormat = Json.format[PaymentMethod]
  implicit val loyaltiesFormat = Json.format[Loyalties]
  implicit val customerInfoFormat = Json.format[CustomerInfo]
  implicit val addressFormat = Json.format[CustomerAddress]
  implicit val storeAddressDetails = Json.format[StoreAddress]
  implicit val storeDetails = Json.format[Store]
  implicit val personDetails = Json.format[Person]
  implicit val pickupStoreDetails = Json.format[PickupStoreDetails]
  implicit val displaySummaryPrice = Json.format[DisplaySummaryPrices]
  implicit val orderSummaryFormat = Json.format[OrderSummary]
  implicit val billingDetailsFormat = Json.format[BillingDetails]
  implicit val ItemResponseFormat = Json.format[ItemResponse]
  implicit val lineTextFormat = Json.format[LineText]
  implicit val giftingsFormat = Json.format[Giftings]
  implicit val orderFormat = Json.format[Order]
}

object Orders {
  implicit val pageInfoFormat = Json.format[PageInfo]
  implicit val ordersFormats = Json.format[Orders]
}

object OrderRequest extends FieldConstraints {
  implicit val orderRequestReads: Reads[OrderRequest] = (
    (__ \ ORDER_NUM).read[String](digitsOnly) and
    (__ \ BILLING_ZIP_CODE).read[String](zip or empty) and
    (__ \ OMS_RESERVATION_ID).readNullable[String](empty or alphaNumeric)
  )(OrderRequest.apply _)

  implicit val orderRequestWrites = Json.writes[OrderRequest]
}
