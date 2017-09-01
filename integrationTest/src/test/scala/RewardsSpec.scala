import java.util.Collections

import com.typesafe.config.{Config, ConfigFactory}
import io.restassured.RestAssured._
import io.restassured.http.{Header, Headers}
import org.hamcrest.CoreMatchers._
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsObject, Json}
import io.restassured.module.scala.RestAssuredSupport.AddThenToResponse

/**
  * Created by michelleling on 2/2/17.
  */
class RewardsSpec extends WordSpec with Matchers {

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

  "Saks First Link Resource" should {
    "Saks First Link Action" in {
      useRelaxedHTTPSValidation()

      val userId: String = randomEmail
      val password: String = "Test123?"
      val accountRequest = buildCreateAccountRequest(userId, password)

      //Create a new account
      val createAccountResponse = given().headers(defaultHeaders).body(accountRequest.toString).log().all()
        .when().post(s"$getServiceUrl/account-service/accounts")
      .Then.log.all().statusCode(200).body("errors", is(Collections.emptyList))
      val cookies = createAccountResponse.extract().cookies()

      val accountId: String = createAccountResponse.extract().response().path("response.results.id")

      //Make sure user is not already linked
      given.cookies(cookies).headers(defaultHeaders)
        .when.get(s"$getServiceUrl/account-service/accounts/$accountId/loyalty-program")
        .Then.log.ifValidationFails.statusCode(200)
        .body("response.results.member_info.linked", is(false))

      val updatedEmail = randomEmail
      val saksfirstBody = Json.obj(
        "name"-> "George Aaron",
        "zip"-> "66208",
        "saks_first_number"-> "10000453"
      )
      //Saks First link
      given().cookies(cookies).headers(defaultHeaders).log().all().body(saksfirstBody.toString())
        .when().post(s"$getServiceUrl/account-service/accounts/$accountId/loyalty-program")
        .Then.log.all.statusCode(200).body("errors", is(Collections.emptyList))

      given.cookies(cookies).headers(defaultHeaders)
        .when.get(s"$getServiceUrl/account-service/accounts/$accountId/summary")
        .Then.log.ifValidationFails.statusCode(200)
        .body("response.results.rewards.saksfirst_rewards_data.linked", is(true))

      //Saks First link
      given().cookies(cookies).headers(defaultHeaders).log().all()
        .when().get(s"$getServiceUrl/account-service/accounts/$accountId/loyalty-program")
        .Then.log.ifValidationFails.statusCode(200)
        .body("response.results.member_info.linked", is(true))

//      Commented out because I don't want to spend points every time the test is run but it's good to periodically test
//      val redemptionRequest = Json.obj(
//        "redemption_type"-> "ELECTRONIC_GIFT_CARD",
//        "award_amount"-> 25
//      )
//      //Saks First link
//      given().cookies(cookies).headers(defaultHeaders).log().all().body(redemptionRequest.toString())
//        .when().post(s"$getServiceUrl/account-service/accounts/$accountId/loyalty-program/redeem")
//        .Then.log.all.statusCode(200)
//        .body("errors", is(Collections.emptyList))
//        .body("response.results.success", is(true))

    }
  }

  def randomEmail: String = {
    String.valueOf(java.util.UUID.randomUUID).substring(0, 8)  + "@email.test"
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
