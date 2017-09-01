package integration.controllers

import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import play.Logger
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class ApplicationSpec extends WordSpec with Matchers with MockitoSugar with BeforeAndAfterAll {

  import utils.TestUtils._

  var application = createIsolatedApplication().build()

  "Application Controller" should {
    "send 404 on a bad request" in {
      val result = route(application, FakeRequest(GET, "/boom")).get
      status(result) shouldBe NOT_FOUND
    }

    "render the index page" in {
      val index = route(application, FakeRequest(GET, versionCtx + "/account-service")).get

      status(index) shouldBe OK
      contentType(index).get == "application/json" shouldBe true
      (contentAsJson(index) \ "response" \ "results").as[String] == "account-service is up and running!" shouldBe true
    }

    "get Swagger spec" in {
      val index = route(application, FakeRequest(GET, versionCtx + "/account-service/api-docs")).get

      status(index) shouldBe OK
      contentType(index).get == "application/json" shouldBe true
      (contentAsJson(index) \ "swagger").as[String] == "2.0" shouldBe true
    }

    "clears country cache" in {
      val index = route(application, FakeRequest(DELETE, versionCtx + "/account-service/clear-country-cache")).get

      status(index) shouldBe OK
      contentType(index).get == "application/json" shouldBe true
      (contentAsJson(index) \ "response" \ "results").as[String] shouldBe "cache cleared!"
    }

    "change the log Level" in {
      val changeLog = route(application, FakeRequest(GET, versionCtx + "/account-service/logLevel/WARN")).get
      status(changeLog) shouldBe OK
      contentType(changeLog).get == "application/json" shouldBe true
      (contentAsJson(changeLog) \ "response" \ "results").as[String] == "Log level changed to WARN" shouldBe true
      Logger.isDebugEnabled shouldBe false
      Await.result(route(application, FakeRequest(GET, versionCtx + "/account-service/logLevel/DEBUG")).get, 10 seconds)
      Logger.isDebugEnabled shouldBe true
    }

    "not process incorrect log Level" in {
      val result = route(application, FakeRequest(GET, versionCtx + "/account-service/logLevel/WARN2")).get
      status(result) shouldBe NOT_FOUND
    }
  }
}
