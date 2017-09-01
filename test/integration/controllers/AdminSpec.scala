package integration.controllers

import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import play.api.test.Helpers._
import play.api.test._

class AdminSpec extends WordSpec
    with Matchers with BeforeAndAfterAll with MockitoSugar {

  import utils.TestUtils._

  var application = createIsolatedApplication().build()

  "Admin controller" should {
    "return healthcheck status" in {
      val ping = route(application, FakeRequest(GET, versionCtx + "/account-service/admin/ping")).get

      status(ping) shouldBe OK
      (contentAsJson(ping) \ "response" \ "results").as[String] shouldBe "pong"
    }

    "show **JVM Stats** when /account-service/admin/jvmstats endpoint is called" in {
      val jvmstats = route(application, FakeRequest(GET, versionCtx + "/account-service/admin/jvmstats")).get

      status(jvmstats) shouldBe OK
      contentAsString(jvmstats).contains("jvm_num_cpus") shouldBe true
    }
  }
}
