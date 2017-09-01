package builders.requests

import play.api.libs.json.{ JsBoolean, JsObject, JsString, JsValue }

class AddressRequestBuilder {

  var userId: String = "abc@abc.com"
  var addressType: String = "billing"
  var company: String = "HBC"
  var address1: String = "123 West st"
  var address2: String = "Apt 2"
  var city: String = "NYC"
  var state: String = "PA"
  var country: String = "US"
  var email: String = "abc@def.com"
  var phone: String = "1234567"
  var zip: String = "11145"
  var firstName: String = "Smith"
  var lastName: String = "Brade"
  var withoutZipField: Boolean = false

  def build(): JsValue = {
    var addressRequest: JsObject = JsObject(Seq(
      ("user_id", JsString(userId)),
      ("address_type", JsString(addressType)),
      ("first_name", JsString(firstName)),
      ("last_name", JsString(lastName)),
      ("company", JsString(company)),
      ("address1", JsString(address1)),
      ("address2", JsString(address2)),
      ("city", JsString(city)),
      ("country", JsString(country)),
      ("state", JsString(state)),
      ("is_default", JsBoolean(true)),
      ("email", JsString(email)),
      ("phone", JsString(phone))
    ))
    if (!withoutZipField) {
      addressRequest += ("zip" -> JsString(zip))
    }
    addressRequest
  }

  def withCountry(country: String): AddressRequestBuilder = {
    this.country = country
    this
  }

  def withCity(city: String): AddressRequestBuilder = {
    this.city = city
    this
  }

  def withState(state: String): AddressRequestBuilder = {
    this.state = state
    this
  }

  def withZip(zip: String): AddressRequestBuilder = {
    this.zip = zip
    this
  }

  def withoutZip(): AddressRequestBuilder = {
    this.withoutZipField = true
    this
  }

  def withFirstName(firstName: String): AddressRequestBuilder = {
    this.firstName = firstName
    this
  }

  def withLastName(lastName: String): AddressRequestBuilder = {
    this.lastName = lastName
    this
  }

  def withAddress(address1: String): AddressRequestBuilder = {
    this.address1 = address1
    this
  }

  def withAddressType(addressType: String) = {
    this.addressType = addressType
    this
  }

  def withAllRequiredFieldsMissing(): AddressRequestBuilder = {
    this.firstName = ""
    this.lastName = ""
    this.address1 = ""
    this.country = ""
    this.city = ""
    this.phone = ""
    this.userId = ""
    this.addressType = ""
    this
  }
}
