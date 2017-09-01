import java.util.Collections

import com.typesafe.config.{Config, ConfigFactory}
import io.restassured.RestAssured._
import io.restassured.http.{Header, Headers}
import io.restassured.module.scala.RestAssuredSupport.AddThenToResponse
import org.hamcrest.CoreMatchers._
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json._

import scala.collection.JavaConversions._

class AccountsSpec extends WordSpec with Matchers {

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

  "Accounts Resource" should {
    "Check the account creation page, create an account, update it, retrieve it, and get the summary" in {
      useRelaxedHTTPSValidation()

      //Get account creation page
      given().headers(defaultHeaders).log().all()
        .when().get(getServiceUrl + "/account-service/accounts")
        .Then.log.ifValidationFails().statusCode(200).body("errors", is(Collections.emptyList))

      //Create a new account
      val createAccountResponse = createAccount(randomEmail, "Test123?")
      val cookies = createCookiesHeader(createAccountResponse.extract().cookies().toMap)

      val accountId: String = createAccountResponse.extract().response().path("response.results.id")

      val updatedEmail = randomEmail
      val accountUpdateBody = Json.obj(
        "first_name" -> "Bill",
        "last_name" -> "Murray",
        "email" -> updatedEmail
      )

      //Update account
      given().header("Cookie", cookies).headers(defaultHeaders).log().all().body(accountUpdateBody.toString())
        .when().put(s"$getServiceUrl/account-service/accounts/$accountId")
        .Then.log.all.statusCode(200).body("errors", is(Collections.emptyList))
        .body("response.results.profile.first_name", is("Bill"))
        .body("response.results.profile.last_name", is("Murray"))
        .body("response.results.profile.email", is(updatedEmail))

      //Retrieve account
      given.log.all.header("Cookie", cookies).headers(defaultHeaders)
        .when.get(s"$getServiceUrl/account-service/accounts/$accountId")
        .Then.log.ifValidationFails.statusCode(200).body("errors", is(Collections.emptyList))
        .body("response.results.first_name", is("Bill"))
        .body("response.results.last_name", is("Murray"))
        .body("response.results.email", is(updatedEmail))

      //Get account summary method
      given.log.all.header("Cookie", cookies).headers(defaultHeaders)
        .when.get(s"$getServiceUrl/account-service/accounts/$accountId/summary")
        .Then.log.ifValidationFails.statusCode(200).body("errors", is(Collections.emptyList))
    }

    "Create an account, then get the account page using the account ID from the cookie from create account" in {
      val createAccountResponse = createAccount(randomEmail, "Test123?")
      val accountId: String = createAccountResponse.extract().cookies().get("AccountId")

      accountId != null should be(true)

      val cookies = createCookiesHeader(createAccountResponse.extract().cookies().toMap)

      //Retrieve account
      given.log.all.header("Cookie", cookies).headers(defaultHeaders)
        .when.get(s"$getServiceUrl/account-service/accounts/$accountId")
        .Then.log.ifValidationFails.statusCode(200).body("errors", is(Collections.emptyList))
    }

    "Create an account, change the password, and still be able to login and get summary" in {
      val email: String = randomEmail
      //Create a new account
      val createAccountResponse = createAccount(email, "Test123?")
      val accountId: String = createAccountResponse.extract().cookies().get("AccountId")
      val cookies = createCookiesHeader(createAccountResponse.extract().cookies().toMap)

      val accountUpdateBody = Json.obj(
        "old_password" -> "Test123?",
        "new_password" -> "NewTest123?",
        "confirm_password" -> "NewTest123?"
      )

      //Change password
      given().header("Cookie", cookies).headers(defaultHeaders).log().all().body(accountUpdateBody.toString())
        .when().post(s"$getServiceUrl/account-service/accounts/$accountId/settings/change-password")
        .Then.log.all.statusCode(200).body("errors", is(Collections.emptyList))

      val signInBody = Json.obj(
        "username" -> email,
        "password" -> "NewTest123?"
      )

      //sign in
      val signInResponse = given().headers(defaultHeaders).log().all().body(signInBody.toString())
        .when().post(s"$getServiceUrl/account-service/accounts/sign-in")
        .Then.log.all.statusCode(200).body("errors", is(Collections.emptyList))

      //Get account summary method
      val newCookies: String = createCookiesHeader(signInResponse.extract().cookies().toMap)
      given.log.all.header("Cookie", newCookies).headers(defaultHeaders)
        .when.get(s"$getServiceUrl/account-service/accounts/$accountId/summary")
        .Then.log.ifValidationFails.statusCode(200).body("errors", is(Collections.emptyList))
    }

    "Create an account, change the email and still be able to login and get summary" in {
      useRelaxedHTTPSValidation()

      val email: String = randomEmail
      //Create a new account
      val createAccountResponse = createAccount(email, "Test123?")
      val accountId: String = createAccountResponse.extract().cookies().get("AccountId")
      val cookies = createCookiesHeader(createAccountResponse.extract().cookies().toMap)

      //Change email
      val newEmail: String = randomEmail
      val accountUpdateBody = Json.obj(
        "first_name" -> "tom",
        "last_name" -> "bomb",
        "email" -> newEmail
      )
      given().header("Cookie", cookies).headers(defaultHeaders).log().all().body(accountUpdateBody.toString())
        .when().put(s"$getServiceUrl/account-service/accounts/$accountId")
        .Then.log.all.statusCode(200)

      //sign in
      val signInBody = Json.obj(
        "username" -> newEmail,
        "password" -> "Test123?"
      )
      val signInResponse = given().headers(defaultHeaders).log().all().body(signInBody.toString())
        .when().post(s"$getServiceUrl/account-service/accounts/sign-in")
        .Then.log.all.statusCode(200).body("errors", is(Collections.emptyList))

      //Get account summary method
      val newCookies: String = createCookiesHeader(signInResponse.extract().cookies().toMap)
      given.log.all.header("Cookie", newCookies).headers(defaultHeaders)
        .when.get(s"$getServiceUrl/account-service/accounts/$accountId/summary")
        .Then.log.ifValidationFails.statusCode(200).body("errors", is(Collections.emptyList))

      //Create a new account to prepare an email collision
      val anotherEmail = randomEmail
      createAccount(anotherEmail, "Test123?")

      //Now try to change the original account to use the email from the new account
      val illegalAccountUpdateBody = Json.obj(
        "first_name" -> "tom",
        "last_name" -> "bomb",
        "email" -> anotherEmail
      )
      given().header("Cookie", cookies).headers(defaultHeaders).log().all().body(illegalAccountUpdateBody.toString())
        .when().put(s"$getServiceUrl/account-service/accounts/$accountId")
        .Then.log.all.statusCode(400)
        .body("errors[0].data", is("The e-mail address that you have selected already exists in our system. Please choose a different e-mail address."), "errors[0].error", containsString("email."))

      val lastGoodEmail = randomEmail
      //Now change to a new email and see that email in the response
      val yesAccountUpdateBody = Json.obj(
        "first_name" -> "lazy",
        "last_name" -> "brother",
        "email" -> lastGoodEmail
      )
      given().header("Cookie", cookies).headers(defaultHeaders).log().all().body(yesAccountUpdateBody.toString())
        .when().put(s"$getServiceUrl/account-service/accounts/$accountId")
        .Then.log.all.statusCode(200)
        .body("response.results.profile.user_id", is(lastGoodEmail))
        .body("response.results.profile.email", is(lastGoodEmail))
        .body("response.results.profile.first_name", is("lazy"))
        .body("response.results.profile.last_name", is("brother"))
    }

    "Create an account, then get a failure when trying to create a duplicate account" in {
      val email: String = randomEmail
      createAccount(email, "Test123?")

      //create account with same email
      val accountRequest = buildCreateAccountRequest(email, "Test123?")
      given().headers(defaultHeaders).body(accountRequest.toString).log().all()
        .when().post(getServiceUrl + "/account-service/accounts")
        .Then.log.ifValidationFails.statusCode(400)
        .body("errors[0].data", is("An account already exists for this email address. Please <a href=\"/account/login\">sign in</a> or choose another email address."), "errors[0].error", containsString("email."))
    }
  }

  def createAccount(userId: String, password: String) = {
    val accountRequest = buildCreateAccountRequest(userId, password)

    given().headers(defaultHeaders).body(accountRequest.toString).log().all()
      .when().post(getServiceUrl + "/account-service/accounts")
      .Then.log.all().statusCode(200).body("errors", is(Collections.emptyList))
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
