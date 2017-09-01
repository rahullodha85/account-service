package services

import javax.inject.Inject

import constants.Constants._
import helpers.{ AccountHelper, ConfigHelper, TogglesHelper }
import models.servicemodel.AddressType.{ BILLING, SHIPPING }
import models.servicemodel._
import models.website.{ AddressRequest, AddressWebsiteModel }
import models.{ ApiErrorModel, FailureResponse, SuccessfulResponse }
import monitoring.StatsDClientLike
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Cookie

import scala.concurrent.Future

class AddressService @Inject() (
    httpTransportService: HttpTransportService,
    localizationService:  LocalizationService,
    configHelper:         ConfigHelper,
    accountHelper:        AccountHelper,
    togglesHelper:        TogglesHelper,
    statsDClient:         StatsDClientLike
) {
  val addressServiceUrl: String = configHelper.getStringProp("data-service.address")

  def getAddressBook(headers: Map[String, String], address_type: AddressType, resource: String): Future[(AddressWebsiteModel, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("AddressService.getAddressBook")({
      for {
        res <- httpTransportService.getFromService[Addresses](addressServiceUrl, Some("addresses"), headers, Map(ADDRESS_TYPE -> address_type.addrType))
        countries <- localizationService.cachedCountries("countries")
        addressBook <- addressBookResponse(countries, res, address_type, resource)
      } yield addressBook
    })
  }

  def createAddress(headers: Map[String, String], addressRequest: AddressRequest): Future[(AddressesResponse, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("AddressService.createAddress")({
      val res = httpTransportService.postToService[AddressRequest, Addresses](addressServiceUrl, Some("address"), addressRequest, headers, Map(ADDRESS_TYPE -> addressRequest.address_type.addrType))
      callAddressServiceAndProcessResponse(res)
    })
  }

  def updateAddress(headers: Map[String, String], addressId: String, addressRequest: AddressRequest): Future[(AddressesResponse, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("AddressService.updateAddress")({
      val res = httpTransportService.postToService[AddressRequest, Addresses](addressServiceUrl, Some(s"address/$addressId"), addressRequest, headers, Map(ADDRESS_TYPE -> addressRequest.address_type.addrType))
      callAddressServiceAndProcessResponse(res)
    })
  }

  def deleteAddress(headers: Map[String, String], address_type: AddressType, addressId: String): Future[(AddressesResponse, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    statsDClient.time("AddressService.deleteAddress")({
      val res = httpTransportService.deleteFromService[Addresses](addressServiceUrl, Some(s"address/$addressId"), headers, Map(ADDRESS_TYPE -> address_type.addrType))
      callAddressServiceAndProcessResponse(res)
    })
  }

  private def callAddressServiceAndProcessResponse(response: Future[Either[FailureResponse, SuccessfulResponse[Addresses]]]): Future[(AddressesResponse, Seq[Cookie], Seq[ApiErrorModel], Int)] = {
    response.map {
      case Left(failureResponse)     => (new AddressesResponse(Seq.empty[AddressResponse]), failureResponse.cookies, failureResponse.errors, failureResponse.code)
      case Right(successfulResponse) => (successfulResponse.body.toAddressesResponse, successfulResponse.cookies, Seq.empty[ApiErrorModel], 200)
    }
  }

  //TODO: come back here and fix this mess
  private def addressBookResponse(countries: Map[String, Country], response: Either[FailureResponse, SuccessfulResponse[Addresses]], addressType: AddressType, resource: String) = {
    togglesHelper.getFavoritesToggleState flatMap { addFavoritesTabOpt =>
      togglesHelper.saksFirstPageEnabled map { saksFirstPageEnabled =>
        val addFavoritesTab = addFavoritesTabOpt
        val (header, messages, links) = addressType match {
          case SHIPPING =>
            (accountHelper.getHeader(addFavoritesTab, saksFirstPageEnabled), accountHelper.buildShippingAddressMessages, Some(Map(ADDRESS_RESOURCE_LABEL_LINK_KEY -> resource)))
          case BILLING =>
            (accountHelper.getHeader(addFavoritesTab, saksFirstPageEnabled), accountHelper.buildBillingAddressMessages, Some(Map(ADDRESS_RESOURCE_LABEL_LINK_KEY -> resource)))
        }
        response match {
          case Left(failureResponse) => (AddressWebsiteModel(enabled = false, Seq.empty, header, messages, links, countries), failureResponse.cookies, failureResponse.errors, 200)
          case Right(successfulResponse) =>
            val addressesResponse: Seq[AddressResponse] = successfulResponse.body.addresses.map { address => address.toAddressResponse }
            (AddressWebsiteModel(enabled = true, addressesResponse, header, messages, links, countries), successfulResponse.cookies, Seq.empty[ApiErrorModel], 200)
        }
      }
    }
  }
}
