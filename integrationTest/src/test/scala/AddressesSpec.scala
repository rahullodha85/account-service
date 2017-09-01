import java.util.Collections

import com.typesafe.config.{Config, ConfigFactory}
import io.restassured.RestAssured._
import io.restassured.http.{Header, Headers}
import io.restassured.module.scala.RestAssuredSupport.AddThenToResponse
import org.hamcrest.CoreMatchers._
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{ JsObject, JsString, Json}

import scala.collection.JavaConversions._

/**
  * Created by gwoskob on 1/25/17.
  */
class AddressesSpec extends WordSpec with Matchers {

  def getServiceUrl: String = {
    config.getString("baseUrl")
  }

  def createCookiesHeader(cookies: Map[String, String]): String = {
    cookies.flatMap { cookieKeyValue =>
      s"${cookieKeyValue._1}=${cookieKeyValue._2};"
    }.mkString
  }

  protected var contentType: Header = new Header("Content-Type", "application/json")
  protected var defaultHeaders: Headers = new Headers(contentType)
  protected var config: Config = ConfigFactory.load

  "Addresses Resource" should {
    "Create a shipping address, retrieve it, update it, and delete it" in {
      createRetrieveUpdateAndDeleteAddress("shipping")
    }

    "Create a billing address, retrieve it, update it, and delete it" in {
      createRetrieveUpdateAndDeleteAddress("billing")
    }

    "Handle default shipping addresses" in {
      //Create an address (without explicitly setting it as default
      createTwoAddressesDeleteDefaultThenSeeOtherAddressMarkedAsDefault("shipping")
    }

    "Handle default billing addresses" in {
      //Create an address (without explicitly setting it as default
      createTwoAddressesDeleteDefaultThenSeeOtherAddressMarkedAsDefault("billing")
    }
  }

  def createTwoAddressesDeleteDefaultThenSeeOtherAddressMarkedAsDefault(addressType: String) = {
    useRelaxedHTTPSValidation()

    val userId: String = String.valueOf(java.util.UUID.randomUUID).substring(0, 8) + "@email.test"
    val password: String = "Test123?"
    val accountRequest = buildCreateAccountRequest(userId, password)

    //Create a new account
    val createAccountResponse = given().headers(defaultHeaders).body(accountRequest.toString).log().all()
      .when().post(getServiceUrl + "/account-service/accounts")
      .Then.log.all().statusCode(200).body("errors", is(Collections.emptyList))
    val cookies = createCookiesHeader(createAccountResponse.extract().cookies().toMap)

    val accountId: String = createAccountResponse.extract().response().path("response.results.id")

    val defaultAddress = Json.obj(
      "is_default" -> true,
      "address1" -> "2345 Picabo St",
      "address2" -> "",
      "city" -> "New York",
      "state" -> "NY",
      "zip" -> "10080",
      "country" -> "US",
      "title" -> "",
      "first_name" -> "hoho",
      "middle_name" -> "h",
      "last_name" -> "poipo",
      "phone" -> "1111111111",
      "address_type" -> addressType,
      "company" -> "HBC"
    )

    //Create a address
    val createResponse = given().header("Cookie", cookies).headers(defaultHeaders).log().all().body(defaultAddress.toString())
      .when().post(s"$getServiceUrl/account-service/accounts/$accountId/addresses")
      .Then.log.all.statusCode(200).body("errors", is(Collections.emptyList))

    val defaultAddressId: String = createResponse.extract.response().path("response.results.addresses[0].id")

    val nonDefaultAddress = Json.obj(
      "is_default" -> false,
      "address1" -> "2345 Other St",
      "address2" -> "",
      "city" -> "New York",
      "state" -> "NY",
      "zip" -> "10080",
      "country" -> "US",
      "title" -> "",
      "first_name" -> "hoho",
      "middle_name" -> "h",
      "last_name" -> "poipo",
      "phone" -> "1111111111",
      "address_type" -> addressType,
      "company" -> "HBC"
    )

    //Create a non default address
    given().header("Cookie", cookies).headers(defaultHeaders).log().all().body(nonDefaultAddress.toString())
      .when().post(s"$getServiceUrl/account-service/accounts/$accountId/addresses")
      .Then.log.all.statusCode(200).body("errors", is(Collections.emptyList))

    //Delete default address
    given.log.all.header("Cookie", cookies).headers(defaultHeaders).param("address_type", addressType)
      .when.delete(s"$getServiceUrl/account-service/accounts/$accountId/addresses/$defaultAddressId")
      .Then.log.ifValidationFails.statusCode(200).body("errors", is(Collections.emptyList))

    //Get addresses and check non default address is now default
    given.log.all.header("Cookie", cookies).headers(defaultHeaders).param("address_type", addressType)
      .when.get(s"$getServiceUrl/account-service/accounts/$accountId/addresses")
      .Then.log.ifValidationFails.statusCode(200).body("errors", is(Collections.emptyList))
      .body("response.results.addresses.size()", is(1))
      .body("response.results.addresses[0].is_default", is(true))
      .body("response.results.addresses[0].address1", is("2345 Other St"))
  }

  def createRetrieveUpdateAndDeleteAddress(addressType: String) = {
    useRelaxedHTTPSValidation()

    val userId: String = String.valueOf(java.util.UUID.randomUUID).substring(0, 8) + "@email.test"
    val password: String = "Test123?"
    val accountRequest = buildCreateAccountRequest(userId, password)

    //Create a new account
    val createAccountResponse = given().headers(defaultHeaders).body(accountRequest.toString).log().all()
      .when().post(getServiceUrl + "/account-service/accounts")
      .Then.log.all().statusCode(200).body("errors", is(Collections.emptyList))
    val cookies = createCookiesHeader(createAccountResponse.extract().cookies().toMap)

    val accountId: String = createAccountResponse.extract().response().path("response.results.id")

    val address = Json.obj(
      "is_default"-> true,
      "address1"-> "2345 Picabo St",
      "address2"-> "",
      "city"-> "New York",
      "state"-> "NY",
      "zip"-> "10080",
      "country"-> "US",
      "title"-> "",
      "first_name"-> "hoho",
      "middle_name"-> "h",
      "last_name"-> "poipo",
      "phone"-> "1111111111",
      "address_type"-> addressType,
      "company"-> "HBC"
    )

    //Create a address
    val createResponse = given().header("Cookie", cookies).headers(defaultHeaders).log().all().body(address.toString())
      .when().post(s"$getServiceUrl/account-service/accounts/$accountId/addresses")
      .Then.log.all.statusCode(200).body("errors", is(Collections.emptyList))

    val addressId: String = createResponse.extract.response().path("response.results.addresses[0].id")

    val updatedAddress = address + ("address2" -> JsString("Apt 23"))

    //Update address
    val updateResponse = given().header("Cookie", cookies).headers(defaultHeaders).log().all().body(updatedAddress.toString())
      .when().put(s"$getServiceUrl/account-service/accounts/$accountId/addresses/$addressId")
      .Then.log.all.statusCode(200).body("errors", is(Collections.emptyList))
      .body("response.results.addresses[0].address2", is("Apt 23"))

    //This is necessary because update changes the id (smh)
    val updatedAddressId: String = updateResponse.extract.response().path("response.results.addresses[0].id")

    //Get addresses
    given.log.all.header("Cookie", cookies).headers(defaultHeaders).param("address_type", addressType)
      .when.get(s"$getServiceUrl/account-service/accounts/$accountId/addresses")
      .Then.log.ifValidationFails.statusCode(200).body("errors", is(Collections.emptyList))
      .body("response.results.addresses.size()", is(1))

    //Delete payment method
    given.log.all.header("Cookie", cookies).headers(defaultHeaders).param("address_type", addressType)
      .when.delete(s"$getServiceUrl/account-service/accounts/$accountId/addresses/$updatedAddressId")
      .Then.log.ifValidationFails.statusCode(200).body("errors", is(Collections.emptyList))
      .body("response.results.addresses.size()", is(0))
  }

  private def buildCreateAccountRequest(userId: String, password: String): JsObject = {
    Json.obj(
      "first_name"-> "Test",
      "last_name"-> "User",
      "email"-> userId,
      "password"-> password,
      "confirm_password"-> password,
      "canadian_customer"-> "F",
      "saks_opt_status"-> "F",
      "off5th_opt_status"-> "F",
      "saks_canada_opt_status"-> "F",
      "off5th_canada_opt_status"-> "F"
    )
  }
}
