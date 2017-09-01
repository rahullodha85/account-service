package services

import javax.inject.{ Inject, Singleton }

import constants.Constants._
import helpers.ConfigHelper
import models.servicemodel.Localization.{ PostalCodeRequirement, Region, Country => LocalizationCountry }
import models.servicemodel.{ Country, CountryFormat, District, LocalizationValidations }
import monitoring.StatsDClientLike
import spray.caching.{ Cache, LruCache }

import scala.concurrent.Future

trait Caching {
  val countriesCache: Cache[Map[String, Country]] = LruCache()
}

@Singleton
class LocalizationService @Inject() (localizationClient: LocalizationClient, configHelper: ConfigHelper, statsDClient: StatsDClientLike) extends Caching
    with CountryFormat
    with LocalizationValidations {

  import play.api.libs.concurrent.Execution.Implicits._

  def clear(): Unit = {
    countriesCache.clear()
  }

  def cachedCountries(key: String) = countriesCache(key) {
    statsDClient.time("AccountService.getCountries") {
      all().filter(_.nonEmpty)
    }
  }

  def all(): Future[Map[String, Country]] = {
    val allCountries = getCountries
    val allRegions = getAllRegions
    val requirements = getPostalRequirements

    for {
      countries <- allCountries
      regions <- allRegions
      postalReq <- requirements
    } yield transform(countries, regions, postalReq)
  }

  private def transform(localizationCountries: Seq[LocalizationCountry], states: Map[String, Seq[Region]], requirements: Seq[PostalCodeRequirement]): Map[String, Country] = {
    val countries: Seq[Country] = localizationCountries.map { c => Country(c.country_code, c.country_name, districtsFor(c, states), usShippingEnabled(c), isPostalCodeRequired(requirements, c)) }
    Map(countries map { country => country.code -> country }: _*)
  }

  private def usShippingEnabled(c: LocalizationCountry) = {
    Some(c.shipping_enabled.equals("Y") || c.country_code == US)
  }

  private def isPostalCodeRequired(postalReqs: Seq[PostalCodeRequirement], c: LocalizationCountry) = {
    postalReqs.find(_.code == c.country_code).exists(_.required)
  }

  private def districtsFor(localizationCountry: LocalizationCountry, states: Map[String, Seq[Region]]): Seq[District] = {
    states.find(_._1 == localizationCountry.country_code).map(_._2.map { region =>
      District(region.display, isState(region, localizationCountry.country_code), region.value)
    }).getOrElse(Seq.empty[District])
  }

  private def isState(region: Region, countryCode: String) = {
    countryCode match {
      case US => Some(configHelper.getObjectListProp(STATES).find(_.getString("value") == region.value).exists(_.getBoolean("is_state")))
      case _  => None
    }
  }

  private def getAllRegions: Future[Map[String, Seq[Region]]] = {
    val canadianProvinces: Future[Seq[Region]] = getRegionsBy(CANADA)
    val usStates: Future[Seq[Region]] = getRegionsBy(US)

    for {
      provinces <- canadianProvinces
      statesAndTerritories <- usStates
    } yield Map(US -> statesAndTerritories, CANADA -> provinces)
  }

  private def getPostalRequirements: Future[Seq[PostalCodeRequirement]] = {
    localizationClient.get[PostalCodeRequirement](configHelper.getStringProp(INTERNATIONAL_CHECKOUT_SERVICE) + "/international_checkout/requirements", validatePostalCodeRequirements)
  }

  private def getCountries: Future[Seq[LocalizationCountry]] = {
    localizationClient.get[LocalizationCountry](configHelper.getStringProp(LOCALIZATION_SERVICE) + "/countries", validateLocalizationCountries)
  }

  private def getRegionsBy(code: String): Future[Seq[Region]] = {
    val headers: (String, String) = ("Cookie", s"$E4X_COUNTRY=$code;$COUNTRY=$code")
    localizationClient.get[Region](configHelper.getStringProp(LOCALIZATION_SERVICE) + "/regions", validateRegions, Some(headers))
  }
}
