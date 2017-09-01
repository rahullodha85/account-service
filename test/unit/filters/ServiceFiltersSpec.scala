package unit.filters

import _root_.helpers.ConfigHelper
import constants.Constants.GENERIC_ERROR
import models.UnauthorizedException
import org.mockito.Matchers.any
import org.mockito.Mockito.{ mock => _, _ }
import org.scalatest.{ BeforeAndAfterEach, Matchers, WordSpec }
import play.api.http.HttpVerbs.GET
import play.api.inject._
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AccountService
import utils.TestUtils._

import scala.language.postfixOps

class FiltersSpec
    extends WordSpec
    with BeforeAndAfterEach
    with Matchers {

  val actionTimeout = ConfigHelper.getIntProp("controllers.timeout")
  val accountService = mock[AccountService]
  val application = createIsolatedApplication().overrides(bind[AccountService].toInstance(accountService)).build

  override def beforeEach(): Unit = {
    reset(accountService)
  }

  "ServiceFilters" should {

    "handle exception when it's thrown by controller" in {
      when(accountService.getAccountSummary(any())(any())).thenThrow(new RuntimeException("this message should not be displayed"))

      val result = route(application, FakeRequest(GET, "/v1/account-service/accounts/123/summary").withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
      status(result) shouldBe 500
      ((contentAsJson(result) \ "errors")(0) \ "error").as[String] should be(GENERIC_ERROR)
    }

    "handle UnauthorizedException when it's thrown by transport" in {
      when(accountService.getAccountSummary(any())(any())).thenThrow(new UnauthorizedException("not logged in", Seq()))

      val result = route(application, FakeRequest(GET, "/v1/account-service/accounts/123/summary").withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc"))).get
      status(result) shouldBe 401
      ((contentAsJson(result) \ "errors")(0) \ "error").as[String] should be("UnauthorizedException")
    }
  }
}
