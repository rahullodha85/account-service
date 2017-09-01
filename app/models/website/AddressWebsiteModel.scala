package models.website

import constants.Constants.{ ADDRESS_ID => _, ADDRESS_TYPE => _, CANADA => _, COUNTRY => _, USER_ID => _ }
import helpers.ConfigHelper
import models.Constants._
import models.FieldConstraints
import models.servicemodel._
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{ JsError, _ }

case class AddressWebsiteModel(
  enabled:   Boolean,
  addresses: Seq[AddressResponse],
  header:    Seq[Header],
  messages:  Option[Map[String, String]],
  links:     Option[Map[String, String]],
  countries: Map[String, Country]
)

object AddressWebsiteModel extends CountryFormat {
  implicit val addressWebsiteModelFormat = Json.format[AddressWebsiteModel]
}

case class AddressRequest(
  id:           Option[Long],
  is_default:   Boolean,
  address_type: AddressType,
  address1:     String,
  address2:     Option[String],
  city:         String,
  state:        Option[String],
  zip:          Option[String],
  country:      String,
  title:        Option[String],
  first_name:   String,
  middle_name:  Option[String],
  last_name:    String,
  phone:        String,
  company:      Option[String]
)

object AddressRequest extends FieldConstraints {

  implicit val addressRequestReads: Reads[AddressRequest] = (
    (__ \ ADDRESS_ID).readNullable[Long] and
    (__ \ IS_DEFAULT_ADDRESS).read[Boolean] and
    (__ \ ADDRESS_TYPE).read[AddressType] and
    (__ \ ADDRESS1).read[String](any80Characters) and
    (__ \ ADDRESS2).readNullable[String](any40Characters or empty) and
    (__ \ CITY).read[String](any40Characters) and
    (__ \ STATE).readNullable[String](any40Characters or empty) and
    (__ \ ZIP).readNullable[String](zip or empty) and
    (__ \ COUNTRY).read[String](any40Characters) and
    (__ \ TITLE).readNullable[String](title or empty) and
    (__ \ FIRST_NAME).read[String](name) and
    (__ \ MIDDLE_NAME).readNullable[String](name or empty) and
    (__ \ LAST_NAME).read[String](name) and
    (__ \ PHONE).read[String](requiredPhone) and
    (__ \ COMPANY).readNullable[String](any80Characters or empty)
  )(AddressRequest.apply _).flatMap { addressRequest =>
      Reads { _ =>
        addressRequest.country match {
          case CANADA =>
            val validPostalCodeFirstLetters: Seq[String] = ConfigHelper.getStringListProp("canadianPostalCodes." + addressRequest.state.getOrElse(""))
            val zip = addressRequest.zip.getOrElse("")
            val postalCodeIsValid: Boolean = validPostalCodeFirstLetters.exists(letter => {
              zip.toUpperCase().startsWith(letter)
            })
            if (!postalCodeIsValid) {
              JsError(JsPath(List(KeyPathNode("zip"))), ValidationError("canadian_invalid"))
            } else {
              JsSuccess(addressRequest)
            }
          case US =>
            if (addressRequest.zip.exists(_.nonEmpty)) {
              JsSuccess(addressRequest)
            } else {
              JsError(JsPath(List(KeyPathNode("zip"))), ValidationError("error.required"))
            }
          case _ =>
            JsSuccess(addressRequest)
        }
      }
    }
  implicit val addressRequestWrites = Json.writes[AddressRequest]
}
