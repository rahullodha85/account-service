package webservices.toggles

import javax.inject.{ Inject, Singleton }

import helpers.ConfigHelper
import models._
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import services.{ HttpTransportService, ServiceException }
import spray.caching._
import net.logstash.logback.argument.StructuredArguments._

import scala.concurrent._
import scala.concurrent.duration._

// no idea about ttl just yet. Do they change much? The service is pretty snappy so could be less aggressive with the cache if useful
// the dev toggle server returns about 260 toggles total right now
trait IndividualToggleCache {
  val toggleCache: Cache[Toggle] = LruCache(maxCapacity = 500, initialCapacity = 275, timeToLive = Duration(24, "hours"))
  def addToCache(toggle: Toggle) = toggleCache(toggle.toggle_name, () => Future.successful(toggle))
}

trait AllTogglesCache {
  val allTogglesCache: Cache[Seq[Toggle]] = LruCache(maxCapacity = 1, initialCapacity = 1, timeToLive = Duration(24, "hours"))
  def addToAllTogglesCache(key: String, toggles: Seq[Toggle]) = allTogglesCache(key, () => Future.successful(toggles))
}

@Singleton
class TogglesClient @Inject() (configHelper: ConfigHelper, httpTransportService: HttpTransportService) extends IndividualToggleCache with AllTogglesCache {

  lazy private val svcUrl = configHelper.getStringProp("webservices.toggles.url")

  private def getCachedToggle(name: String): Future[Toggle] = toggleCache(name) {
    httpTransportService.getFromService[Toggle](svcUrl, Some(name), Map.empty).map {
      case Left(failureResponse) =>
        val errorMessage: String = "Toggle web request failed"
        Logger.error(errorMessage, keyValue("toggleName", name))
        Toggle(name, false)
      case Right(toggle) =>
        toggle.body
    }
  }

  private def getAllToggles(): Future[Seq[Toggle]] =
    httpTransportService.getFromService[Seq[Toggle]](svcUrl, None, Map.empty).map {
      case Left(failureResponse) =>
        Seq.empty[Toggle]
      case Right(toggles) =>
        toggles.body.foreach(addToCache) // shove them in the individual toggle cache
        toggles.body.sortBy(_.toggle_name)
    }

  private def getCachedToggles(): Future[Seq[Toggle]] = allTogglesCache("all") {
    getAllToggles()
  }

  private def clearBothCaches(): Unit = {
    toggleCache.clear
    allTogglesCache.clear
  }

  // ************** Exposed Services ****************************
  def getToggle(name: String): Future[Toggle] = getCachedToggle(name)

  def getToggleState(name: String): Future[Boolean] = getCachedToggle(name).map(_.toggle_state)

  def getToggles(): Future[Seq[Toggle]] = getCachedToggles()

  def clearCache(name: Option[String]) =
    name.fold(clearBothCaches)(k => toggleCache.remove(k))

}
