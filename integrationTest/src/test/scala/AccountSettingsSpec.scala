import java.util.Collections

import com.typesafe.config.{Config, ConfigFactory}
import io.restassured.RestAssured._
import io.restassured.http.{Header, Headers}
import org.hamcrest.CoreMatchers._
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsArray, JsObject, JsString, Json}
import io.restassured.module.scala.RestAssuredSupport.AddThenToResponse

/**
  * Created by michelleling on 2/3/17.
  */
class AccountSettingsSpec extends WordSpec with Matchers {

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

  "Account Settings Resource" should {
    "Check the account settings page, get or update the account, and get or update the email preferences, and change the password" in {
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

      val updatedEmail = randomEmail
      val accountUpdateBody = Json.obj(
        "first_name" -> "Bill",
        "last_name" -> "Murray",
        "email" -> updatedEmail
      )
      val changePwdBody = Json.obj(
        "old_password" -> "Test123?",
        "new_password" -> "NEWTest123?",
        "confirm_password" -> "NEWTest123?"
      )

      val editEmailPrefsBody = Json.obj(
        "preferences" -> Json.toJson(Seq(
          "off5th_opt_status",
          "saks_opt_status",
          "off5th_canada_opt_status")
        )
      )

      //Update account
      given().cookies(cookies).headers(defaultHeaders).log().all().body(accountUpdateBody.toString())
        .when().put(s"$getServiceUrl/account-service/accounts/$accountId")
        .Then.log.all.statusCode(200).body("errors", is(Collections.emptyList))
        .body("response.results.profile.first_name", is("Bill"))
        .body("response.results.profile.last_name", is("Murray"))
        .body("response.results.profile.email", is(updatedEmail))

      //Change password
      given().cookies(cookies).headers(defaultHeaders).log().all().body(changePwdBody.toString())
        .when().post(s"$getServiceUrl/account-service/accounts/$accountId/settings/change-password")
        .Then.log.all.statusCode(200).body("errors", is(Collections.emptyList))

      //Update email prefs.
      given().cookies(cookies).headers(defaultHeaders).log().all().body(editEmailPrefsBody.toString())
        .when().put(s"$getServiceUrl/account-service/accounts/$accountId/settings/email-preferences")
        .Then.log.all.statusCode(200).body("errors", is(Collections.emptyList))
        .body("response.results.preferences[0]", is("off5th_opt_status"))
        .body("response.results.preferences[1]", is("saks_opt_status"))
        .body("response.results.preferences[2]", is("off5th_canada_opt_status"))

      //Get account settings method
      given.log.all.cookies(cookies).headers(defaultHeaders)
        .when.get(s"$getServiceUrl/account-service/accounts/$accountId/settings")
        .Then.log.ifValidationFails.statusCode(200).body("errors", is(Collections.emptyList))
    }
  }

  def randomEmail: String = {
    String.valueOf(java.util.UUID.randomUUID).substring(0, 8) + "@email.test"
  }

  private def buildCreateAccountRequest(userId: String, password: String): JsObject = {
    Json.obj(
      "first_name" -> "Test",
      "last_name" -> "User",
      "email" -> userId,
      "password" -> password,
      "confirm_password" -> password,
      "canadian_customer" -> "F",
      "saks_opt_status" -> "T",
      "off5th_opt_status" -> "T",
      "saks_canada_opt_status" -> "F",
      "off5th_canada_opt_status" -> "F"
    )
  }
}
