import java.util.Collections

import com.typesafe.config.{Config, ConfigFactory}
import io.restassured.RestAssured._
import io.restassured.http.{Header, Headers}
import io.restassured.module.scala.RestAssuredSupport.AddThenToResponse
import org.hamcrest.CoreMatchers.{is, _}
import org.scalatest.{ Matchers, WordSpec}
import play.api.libs.json.{ JsObject, Json }

import collection.JavaConversions._

/**
  * Created by Automation on 2/1/17.
  */
class OrderSpec extends WordSpec with Matchers{

  protected var contentType: Header = new Header("Content-Type", "application/json")
  protected var defaultHeaders: Headers = new Headers(contentType)
  protected var config: Config = ConfigFactory.load

  def getServiceUrl: String = {
    config.getString("baseUrl")
  }

  def createCookiesHeader(cookies: Map[String, String]): String = {
    cookies.flatMap { cookieKeyValue =>
      s"${cookieKeyValue._1}=${cookieKeyValue._2};"
    }.mkString
  }

  "Order Resource" should {
    "get all orders for an account when get order history endpoint is called" in {
      useRelaxedHTTPSValidation()

      val userId: String = String.valueOf(java.util.UUID.randomUUID).substring(0, 8) + "@email.test"
      val password: String = "Test123?"
      val accountRequest = buildCreateAccountRequest(userId, password)

      //Create a new account
      val createAccountResponse = given().headers(defaultHeaders).body(accountRequest.toString).log().all()
        .when().post(getServiceUrl + "/account-service/accounts")
        .Then.log.all().statusCode(200).body("errors", is(Collections.emptyList))
      val cookies = createAccountResponse.extract().cookies().toMap

      val accountId: String = createAccountResponse.extract().response().path("response.results.id")

      val getOrderHistoryResponse = given().headers(defaultHeaders).cookies(cookies).log.all()
        .when().get(s"$getServiceUrl/account-service/accounts/$accountId/orders")
        .Then.log.ifValidationFails().statusCode(200).body("errors", is(Collections.emptyList))
    }

    "throw 400 bad request exception when billing_zip_code is missing in get order details request" in {
      val getOrderDetailsResponse = given().headers(defaultHeaders).log.all()
        .when.get(s"$getServiceUrl/account-service/orders/12345")
        .Then.log.ifValidationFails().statusCode(400)
    }

    "throw 404 exception when user enters incorrect order_id and/or billing_zipcode in get order details request" in {
      val getOrderDetailsResponse = given().headers(defaultHeaders).log.all()
        .when.get(s"$getServiceUrl/account-service/orders/12345?billing_zip_code=11111")
        .Then.log.ifValidationFails().statusCode(404)
    }

    //Requires backend change to fix
    "throw 404 exception when user enters incorrect order)id and/or billing_zip_code in cancel order request" in {

      val jsonBody = Json.obj(
        "order_num" -> "12345",
        "billing_zip_code" -> "11111"
      )

      val getOrderDetailsResponse = given().headers(defaultHeaders).body(jsonBody.toString).log.all()
        .when.put(s"$getServiceUrl/account-service/orders/12345")
        .Then.log.ifValidationFails().statusCode(404)
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
