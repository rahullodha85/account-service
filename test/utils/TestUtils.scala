package utils

import com.typesafe.config.ConfigFactory
import models.servicemodel.Country
import monitoring.StatsDClientLike
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.Configuration
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import services.LocalizationService

import scala.concurrent.Future

object TestUtils extends MockitoSugar {
  val configString = """
                       | play.crypto.secret="SECRET"
                       | logger.application=ERROR
                       | play.i18n.langs=["en"]
                       | play.http.filters=filters.ServiceFilters
                       | controllers.timeout=10000
                       | akka {
                       |   akka.loggers = ["akka.event.slf4j.Slf4jLogger"]
                       |   loglevel = ERROR
                       | }
                     """.stripMargin

  val config = ConfigFactory.parseString(configString)

  val configuration = new Configuration(config)

  val versionCtx = "/v1"

  val localizationService = mock[LocalizationService]
  when(localizationService.cachedCountries(any[String])).thenReturn(Future.successful(Map.empty[String, Country]))
  when(localizationService.all()).thenReturn(Future.successful(Map.empty[String, Country]))

  def createIsolatedApplication(
    config: Configuration = configuration
  ) = {
    new GuiceApplicationBuilder().configure(config)
      .overrides(bind[StatsDClientLike].toInstance(NoOpStatsDClient))
      .overrides(bind[LocalizationService].toInstance(localizationService))
  }
}
