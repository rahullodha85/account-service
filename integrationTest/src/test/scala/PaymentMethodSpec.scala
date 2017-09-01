import java.util.Collections

import com.typesafe.config.{Config, ConfigFactory}
import io.restassured.RestAssured._
import io.restassured.http.{Header, Headers}
import io.restassured.module.scala.RestAssuredSupport.AddThenToResponse
import org.hamcrest.CoreMatchers._
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsNumber, JsObject, JsString, Json}

import collection.JavaConversions._

/**
  * Created by gwoskob on 1/25/17.
  */
class PaymentMethodSpec extends WordSpec with Matchers {

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

  "Payment Method Resource" should {
    "Create a payment method, retrieve it, update it, and delete it" in {
      useRelaxedHTTPSValidation()

      val userId: String = String.valueOf(java.util.UUID.randomUUID).substring(0, 8) + "@email.test"
      val password: String = "Test123?"
      val accountRequest = buildCreateAccountRequest(userId, password)

      //Create a new account
      val createAccountResponse = given().headers(defaultHeaders).body(accountRequest.toString).log().all()
        .when().post(getServiceUrl + "/account-service/accounts")
        .Then.log.all().statusCode(200).body("errors", is(Collections.emptyList))
      val cookies = createCookiesHeader(createAccountResponse.extract().cookies().toMap)

      val paymentMethod = Json.obj(
        "brand" -> "DISC",
        "is_default" -> true,
        "month" -> 4,
        "year" -> 2030,
        "name" -> "The Dude",
        "number" -> "6011000990911111",
        "security_code" -> ""
      )

      val accountId: String = createAccountResponse.extract().response().path("response.results.id")

      //Create a payment method
      val createResponse = given().header("Cookie", cookies).headers(defaultHeaders).log().all().body(paymentMethod.toString())
        .when().post(s"$getServiceUrl/account-service/accounts/$accountId/payment-methods")
        .Then.log.all.statusCode(200).body("errors", is(Collections.emptyList))

      val paymentMethodId: Int = createResponse.extract.response().path("response.results.payment_methods_info[0].credit_card.id")

      val updatedPaymentMethod = paymentMethod + ("name" -> JsString("Big Lebowski")) + ("id" -> JsNumber(paymentMethodId))

      //Update payment method
      val updateResponse = given().header("Cookie", cookies).headers(defaultHeaders).log().all().body(updatedPaymentMethod.toString())
        .when().put(s"$getServiceUrl/account-service/accounts/$accountId/payment-methods/$paymentMethodId")
        .Then.log.all.statusCode(200).body("errors", is(Collections.emptyList))
        .body("response.results.payment_methods_info[0].credit_card.name", is("Big Lebowski"))

      //This is necessary because updated changes the id (smh)
      val updatedPaymentMethodId: Int = updateResponse.extract.response().path("response.results.payment_methods_info[0].credit_card.id")

      //Get payment methods
      given.log.all.header("Cookie", cookies).headers(defaultHeaders)
        .when.get(s"$getServiceUrl/account-service/accounts/$accountId/payment-methods")
        .Then.log.ifValidationFails.statusCode(200).body("errors", is(Collections.emptyList))
        .body("response.results.payment_methods_info.size()", is(1))

      //Delete payment method
      given.log.all.header("Cookie", cookies).headers(defaultHeaders)
        .when.delete(s"$getServiceUrl/account-service/accounts/$accountId/payment-methods/$updatedPaymentMethodId")
        .Then.log.ifValidationFails.statusCode(200).body("errors", is(Collections.emptyList))
        .body("response.results.payment_methods_info.size()", is(0))
    }

    "Handle default payment methods" in {
      useRelaxedHTTPSValidation()

      //Create an address (without explicitly setting it as default
      val userId: String = String.valueOf(java.util.UUID.randomUUID).substring(0, 8) + "@email.test"
      val password: String = "Test123?"
      val accountRequest = buildCreateAccountRequest(userId, password)

      //Create a new account
      val createAccountResponse = given().headers(defaultHeaders).body(accountRequest.toString).log().all()
        .when().post(getServiceUrl + "/account-service/accounts")
        .Then.log.all().statusCode(200).body("errors", is(Collections.emptyList))
      val cookies = createCookiesHeader(createAccountResponse.extract().cookies().toMap)

      val accountId: String = createAccountResponse.extract().response().path("response.results.id")

      val paymentMethod = Json.obj(
        "brand" -> "DISC",
        "is_default" -> true,
        "month" -> 4,
        "year" -> 2030,
        "name" -> "The Dude",
        "number" -> "6011000990911111",
        "security_code" -> ""
      )

      //Create a payment method
      val createResponse = given().header("Cookie", cookies).headers(defaultHeaders).log().all().body(paymentMethod.toString())
        .when().post(s"$getServiceUrl/account-service/accounts/$accountId/payment-methods")
        .Then.log.all.statusCode(200).body("errors", is(Collections.emptyList))

      val defaultPaymentMethodId: Int = createResponse.extract.response().path("response.results.payment_methods_info[0].credit_card.id")

      val nonDefaultPaymentMethod = Json.obj(
        "brand" -> "VISA",
        "is_default" -> false,
        "month" -> 4,
        "year" -> 2030,
        "name" -> "The Lady",
        "number" -> "4111111111111111",
        "security_code" -> ""
      )

      //Create a payment method
      given().header("Cookie", cookies).headers(defaultHeaders).log().all().body(nonDefaultPaymentMethod.toString())
        .when().post(s"$getServiceUrl/account-service/accounts/$accountId/payment-methods")
        .Then.log.all.statusCode(200).body("errors", is(Collections.emptyList))

      //Delete default payment method
      given.log.all.header("Cookie", cookies).headers(defaultHeaders)
        .when.delete(s"$getServiceUrl/account-service/accounts/$accountId/payment-methods/$defaultPaymentMethodId")
        .Then.log.ifValidationFails.statusCode(200).body("errors", is(Collections.emptyList))

      //Get payment methods and check non default address is now default
      given.log.all.header("Cookie", cookies).headers(defaultHeaders)
        .when.get(s"$getServiceUrl/account-service/accounts/$accountId/payment-methods")
        .Then.log.ifValidationFails.statusCode(200).body("errors", is(Collections.emptyList))
        .body("response.results.payment_methods_info.size()", is(1))
        .body("response.results.payment_methods_info[0].credit_card.is_default", is(true))
        .body("response.results.payment_methods_info[0].credit_card.name", is("The Lady"))
    }
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
