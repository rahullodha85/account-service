package models.servicemodel

import models.website.Header
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait AddressType { def addrType: String }

object AddressType {
  case object SHIPPING extends AddressType { val addrType = "shipping" }
  case object BILLING extends AddressType { val addrType = "billing" }

  implicit val format = new Format[AddressType] {
    def reads(json: JsValue): JsResult[AddressType] = json match {
      case JsString(AddressType.SHIPPING.addrType) => JsSuccess(AddressType.SHIPPING)
      case JsString(AddressType.BILLING.addrType)  => JsSuccess(AddressType.BILLING)
      case _                                       => JsError(Seq(JsPath() -> Seq(ValidationError("invalid.address.type"))))
    }
    def writes(addrType: AddressType): JsValue = JsString(addrType.addrType)
  }
}

case class Address(
    id:          Option[Long],
    user_id:     String,
    is_default:  Boolean,
    address1:    String,
    address2:    Option[String],
    city:        String,
    state:       String,
    zip:         Option[String],
    country:     String,
    title:       String,
    first_name:  String,
    middle_name: Option[String],
    last_name:   String,
    phone:       String,
    company:     Option[String]
) {
  def toAddressResponse: AddressResponse = {
    AddressResponse(id.getOrElse(0L).toString, user_id, is_default, address1, address2, city, state, zip, country, title, first_name, middle_name, last_name, phone, company)
  }
}

case class AddressResponse(
  id:          String,
  user_id:     String,
  is_default:  Boolean,
  address1:    String,
  address2:    Option[String],
  city:        String,
  state:       String,
  zip:         Option[String],
  country:     String,
  title:       String,
  first_name:  String,
  middle_name: Option[String],
  last_name:   String,
  phone:       String,
  company:     Option[String]
)

case class Addresses(addresses: Seq[Address]) {
  def toAddressesResponse: AddressesResponse = {
    AddressesResponse(
      addresses.map { address =>
        address.toAddressResponse
      }
    )
  }
}

case class AddressesResponse(addresses: Seq[AddressResponse])

object AddressResponse {
  implicit val addressResponseFormat = Json.format[AddressResponse]
}

object AddressesResponse {
  implicit val addressesResponseFormat = Json.format[AddressesResponse]
}

object Address {
  implicit val addressFormat = Json.format[Address]
}

object Addresses {
  implicit val addressesFormat = Json.format[Addresses]
}