import java.util.Collections

import com.typesafe.config.{Config, ConfigFactory}
import io.restassured.RestAssured._
import io.restassured.http.{Header, Headers}
import io.restassured.module.scala.RestAssuredSupport.AddThenToResponse
import org.hamcrest.CoreMatchers._
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json._

import scala.collection.JavaConversions._

/**
  * Created by gwoskob on 1/25/17.
  */
class SignInSpec extends WordSpec with Matchers {

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

  "Sign In Resource" should {
    "Create an account, get sign in page, log into it, grab the accountId from the cookies of that response, check a protected page, then sign out, and get a 401 on the same protected page" in {
      useRelaxedHTTPSValidation()

      val userId: String = randomEmail
      val password: String = "Test123?"
      val accountRequest = buildCreateAccountRequest(userId, password)

      //Create a new account
      given().headers(defaultHeaders).body(accountRequest.toString).log().all()
        .when().post(getServiceUrl + "/account-service/accounts")
        .Then.log.all().statusCode(200).body("errors", is(Collections.emptyList))

      //Get sign in page
      given().headers(defaultHeaders).log().all()
        .when().get(getServiceUrl + "/account-service/accounts/sign-in")
        .Then.log.ifValidationFails().statusCode(200).body("errors", is(Collections.emptyList))

      val signInPostBody = Json.obj(
        "username" -> userId,
        "password" -> password
      )

      //Sign in
      val signInResponse = given().headers(defaultHeaders).log().all().body(signInPostBody.toString())
        .when().post(s"$getServiceUrl/account-service/accounts/sign-in")
        .Then.log.all.statusCode(200).body("errors", is(Collections.emptyList))

      val cookies = signInResponse.extract().cookies().toMap
      val accountId = cookies.get("AccountId").get
      val cookiesString = createCookiesHeader(cookies)

      //Retrieve account successfully
      given.log.all.header("Cookie", cookiesString).headers(defaultHeaders)
        .when.get(s"$getServiceUrl/account-service/accounts/$accountId")
        .Then.log.ifValidationFails.statusCode(200).body("errors", is(Collections.emptyList))

      //Get account summary method
      given.log.all.header("Cookie", cookiesString).headers(defaultHeaders)
        .when.get(s"$getServiceUrl/account-service/accounts/$accountId/sign-out")
        .Then.log.ifValidationFails.statusCode(200).body("errors", is(Collections.emptyList))

      //Retrieve account and get unauthorized
      given.log.all.header("Cookie", cookiesString).headers(defaultHeaders)
        .when.get(s"$getServiceUrl/account-service/accounts/$accountId")
        .Then.log.ifValidationFails.statusCode(401)
        .body("errors[0].data", is("User is not logged in"))
        .body("errors[0].error", is("UnauthorizedException"))
    }
  }

  def randomEmail: String = {
    String.valueOf(java.util.UUID.randomUUID).substring(0, 8) + "@email.test"
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
