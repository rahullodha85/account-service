package unit.helpers

import helpers.RequestHelper
import models.UnauthorizedException
import models.website.{ AccountTitleObject, SignedInWebsiteModel }
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }
import play.api.mvc.{ Cookie, Result, Results }
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AccountService

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

class RequestHelperSpec extends WordSpec
    with MockitoSugar
    with Matchers {

  "Authorized" should {

    "return unauthorized error on missing JsessionId cookie" in {
      val result = Await.result(
        RequestHelper.Authorized((headers, request) => Future.successful(Results.Status(200)))(FakeRequest(GET, "/any_route").withCookies(Cookie("UserName", "abc"))), Duration("5 seconds")
      )
      result.header.status shouldBe UNAUTHORIZED
    }

    "return authorized even on missing userName cookie" in {
      val result = Await.result(
        RequestHelper.Authorized((headers, request) => Future.successful(Results.Status(200)))(FakeRequest(GET, "/any_route").withCookies(Cookie("JSESSIONID", "123"))), Duration("5 seconds")
      )
      result.header.status should not be UNAUTHORIZED
    }
  }

  "AuthorizedWithNoBM" should {

    "call account service to see if user is authorized" in {
      val service = mock[AccountService]
      when(service.getAccount(any[Map[String, String]])).thenThrow(UnauthorizedException("", Seq()))

      an[UnauthorizedException] should be thrownBy {
        RequestHelper.AuthorizedWithNoBM(service, "", (userName, headers, request) => Future.successful(Results.Status(200)))(FakeRequest(GET, "/any_route").withCookies(Cookie("JSESSIONID", "123"), Cookie("UserName", "abc")))
      }
    }

    "return unauthorized error on missing JsessionId cookie" in {
      val service = mock[AccountService]

      val result = Await.result(
        RequestHelper.AuthorizedWithNoBM(service, "", (userName, headers, request) => Future.successful(Results.Status(200)))(FakeRequest(GET, "/any_route").withCookies(Cookie("UserName", "abc"))), Duration("5 seconds")
      )
      result.header.status shouldBe UNAUTHORIZED
    }

    "return authorized when everything is ok" in {
      val service = mock[AccountService]

      val successfulAuthCall = Future.successful((Some(SignedInWebsiteModel(Some("12345"), None, "", "", None, "", None, None, AccountTitleObject("", ""))), Seq.empty, Seq.empty, 200))
      when(service.getAccount(any[Map[String, String]])).thenReturn(successfulAuthCall)

      val result = Await.result(
        RequestHelper.AuthorizedWithNoBM(service, "12345", (userName, headers, request) => Future.successful(Results.Status(200)))(FakeRequest(GET, "/any_route").withCookies(Cookie("JSESSIONID", "123"))), Duration("5 seconds")
      )
      result.header.status shouldBe OK
    }

    "return unauthorized when account id in parameter does not match what account service returns" in {
      val service = mock[AccountService]

      val successfulAuthCall = Future.successful((Some(SignedInWebsiteModel(Some("12345"), None, "", "", None, "", None, None, AccountTitleObject("", ""))), Seq.empty, Seq.empty, 200))
      when(service.getAccount(any[Map[String, String]])).thenReturn(successfulAuthCall)

      val response: Future[Result] = RequestHelper.AuthorizedWithNoBM(service, "67890", (userName, headers, request) => Future.successful(Results.Status(200)))(FakeRequest(GET, "/any_route").withCookies(Cookie("JSESSIONID", "123")))
      whenReady(response.failed) { actualResponse =>
        actualResponse shouldBe a[UnauthorizedException]
      }
    }
  }
}
