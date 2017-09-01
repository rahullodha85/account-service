package integration

import com.typesafe.config.{Config, ConfigFactory}
import io.restassured.RestAssured.useRelaxedHTTPSValidation
import io.restassured.RestAssured._
import io.restassured.module.scala.RestAssuredSupport.AddThenToResponse
import io.restassured.http.{Header, Headers}
import org.scalatest.{Matchers, WordSpec}
import org.hamcrest.CoreMatchers._
import play.api.libs.json.Json

/**
  * Created by Automation on 2/3/17.
  */
class ResetPasswordSpec extends WordSpec with Matchers {
  protected var contentType: Header = new Header("Content-Type", "application/json")
  protected var defaultHeaders: Headers = new Headers(contentType)
  protected var config: Config = ConfigFactory.load

  def getServiceUrl: String = {
    config.getString("baseUrl")
  }

  "Reset-Password Resource" should {
    "return new forgot password link url when forgot password page end-point is called" in {
      useRelaxedHTTPSValidation()

      val forgotPasswordResponse = given().headers(defaultHeaders).log.all()
        .when().get(s"$getServiceUrl/account-service/accounts/forgot-password")
        .Then.log().all().statusCode(200)
        .body("response.results.links.forgot_password_action", is("/v1/account-service/accounts/forgot-password"))
    }

    "return new reset password link url when reset password page end-point is called and fail because Loc is not correct" in {
      useRelaxedHTTPSValidation()

      val forgotPasswordResponse = given().headers(defaultHeaders).log.all()
        .when().get(s"$getServiceUrl/account-service/accounts/reset-password?Loc=test")
        .Then.log().all().statusCode(403)
        .body("response.results.links.reset_password_action", is("/v1/account-service/accounts/reset-password?Loc=test"))
    }

    "return a 200 success response when I forgot my password and enter an email that does not exist in the database" in {
      val postBody = Json.obj(
        "email" -> randomEmail
      )
      given.log.all.headers(defaultHeaders).body(postBody.toString())
        .when.post(getServiceUrl + "/account-service/accounts/forgot-password")
        .Then.log().all().statusCode(200)
    }
  }

  def randomEmail: String = {
    String.valueOf(java.util.UUID.randomUUID).substring(0, 8) + "@email.test"
  }
}
