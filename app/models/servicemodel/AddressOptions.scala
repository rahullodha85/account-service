package models.servicemodel

import models.servicemodel.Localization.{ PostalCodeRequirement, Region }
import play.api.libs.json._

case class Country(code: String, name: String, districts: Seq[District], can_ship_to: Option[Boolean], postal_code_required: Boolean)

case class District(name: String, is_state: Option[Boolean], code: String)

object Localization {
  case class Country(country_name: String, country_code: String, currency_name: String, currency_code: String, currency_symbol: String, shipping_enabled: String, billing_in_currency: String)
  case class Region(display: String, value: String)
  case class PostalCodeRequirement(name: String, code: String, required: Boolean)

  implicit val regionFormat = Json.format[Region]
  implicit val postalCodeRequirement = Json.format[PostalCodeRequirement]
  implicit val countryFormat = Json.format[Country]
}

trait LocalizationValidations {
  def validateLocalizationCountries(jsValue: JsValue) = jsValue.validate[Seq[Localization.Country]]
  def validateRegions(jsValue: JsValue) = jsValue.validate[Seq[Region]]
  def validatePostalCodeRequirements(jsValue: JsValue) = jsValue.validate[Seq[PostalCodeRequirement]]
}

trait DistrictFormat {
  implicit val districtFormat = Json.format[District]
}

trait CountryFormat extends DistrictFormat {
  implicit val countryFormat = Json.format[Country]
}
