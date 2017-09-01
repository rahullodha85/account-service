
package unit.service

import helpers.ConfigHelper
import models._
import org.mockito.Matchers.any
import org.mockito.Mockito.{ times, verify, when }
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }
import services.HttpTransportService
import webservices.toggles.TogglesClient

import scala.concurrent._
import scala.concurrent.duration._

class ToggleClientSpec extends WordSpec with Matchers with MockitoSugar {

  val toggle1 = Toggle("TEST_ONE", false)
  val toggle2 = Toggle("TEST_TWO", true)
  val toggle3 = Toggle("TEST_THREE", true)

  val configHelper = mock[ConfigHelper]
  val serviceUrl = "http://hd1dtgl01lx.saksdirect.com:9880/toggle-service/toggles"
  when(configHelper.getStringProp("webservices.toggles.url")).thenReturn(serviceUrl)
  val httpTransportService = mock[HttpTransportService]
  val client = new TogglesClient(configHelper, httpTransportService)

  "Toggle service with spray cache" should {
    "return a toggle already in the cache" in {
      client.addToCache(toggle1)
      client.addToCache(toggle2)
      client.addToCache(toggle3)

      val toggleFuture = client.getToggle("TEST_ONE")
      val toggle = Await.result(toggleFuture, 1.seconds)
      toggle.toggle_state should be(false)
    }

    "delete a single toggle" should {
      client.addToCache(toggle1)
      client.addToCache(toggle2)
      client.addToCache(toggle3)

      val initialSize = client.toggleCache.size
      client.clearCache(Some("TEST_TWO"))
      client.toggleCache.size should be(initialSize - 1)
    }

    "cache should be clearable" in {
      client.addToCache(toggle1)
      client.addToCache(toggle2)
      client.addToCache(toggle3)

      client.clearCache(None)
      client.toggleCache.size should be(0)
    }

    "call transport service if toggle not in cache" in {
      client.clearCache(None)

      client.getToggle("some_toggle")

      verify(httpTransportService, times(1)).getFromService(org.mockito.Matchers.eq(serviceUrl), org.mockito.Matchers.eq(Some("some_toggle")), org.mockito.Matchers.eq(Map.empty), any())(any())
    }
  }
}
