package controllers

import javax.inject.Inject

import helpers.{ ControllerPayload, RequestHelper }
import models.servicemodel.AddressType
import models.website.AddressRequest
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsString
import play.api.mvc.Controller
import services.AddressService
import validations.Validator

import scala.concurrent.Future

class AddressController @Inject() (
    addressService: AddressService,
    validator:      Validator
) extends Controller with ControllerPayload with RequestHelper {

  def getAddresses(account_id: String, address_type: String) = Authorized { (headers, request) =>
    implicit val req = request
    validator.validate[AddressType](Some(JsString(address_type))) match {
      case Right(addressType) =>
        addressService.getAddressBook(headers, addressType, request.path).map { e =>
          writeResponseGet(e._1, e._3).withCookies(e._2: _*)
        }
      case Left(errors) =>
        Future.successful(writeResponseError(errors, Status(400)))
    }
  }

  def createAddress(account_id: String) = Authorized { (headers, request) =>
    implicit val req = request
    validator.validate[AddressRequest](request.body.asJson) match {
      case Right(addressRequest) =>
        addressService.createAddress(request.headers.toSimpleMap, addressRequest).map { e =>
          writeResponse(e)
        }
      case Left(errors) =>
        Future.successful(writeResponseError(errors, Status(400)))
    }
  }

  def updateAddress(account_id: String, address_id: String) = Authorized { (headers, request) =>
    implicit val req = request
    validator.validate[AddressRequest](request.body.asJson) match {
      case Right(addressRequest) =>
        addressService.updateAddress(request.headers.toSimpleMap, address_id, addressRequest).map { e =>
          writeResponse(e)
        }
      case Left(errors) =>
        Future.successful(writeResponseError(errors, Status(400)))
    }
  }

  def deleteAddress(account_id: String, address_id: String, address_type: String) = Authorized { (headers, request) =>
    implicit val req = request
    validator.validate[AddressType](Some(JsString(address_type))) match {
      case Right(addressType) =>
        addressService.deleteAddress(request.headers.toSimpleMap, addressType, address_id).map { e =>
          writeResponse(e)
        }
      case Left(errors) =>
        Future.successful(writeResponseError(errors, Status(400)))
    }
  }
}
