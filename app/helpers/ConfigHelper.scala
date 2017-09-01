package helpers

import com.typesafe.config.{ Config, ConfigFactory }
import constants.Banners

import scala.collection.JavaConverters._
import scala.util.Try

class ConfigHelper {
  lazy val config = ConfigFactory.load()

  val configPrefix: Option[String] = None

  def getParam(param: String) = configPrefix.map { configPrefix => s"$configPrefix.$param" }.getOrElse(param)

  def getStringProp(param: String): String = Try(config.getString(getParam(param))).getOrElse("")
  def getIntProp(param: String): Int = Try(config.getInt(getParam(param))).getOrElse(0)
  def getBooleanProp(param: String): Boolean = Try(config.getBoolean(getParam(param))).getOrElse(false)
  def getStringListProp(param: String): Seq[String] = Try(config.getStringList(getParam(param)).asScala).getOrElse(Seq())
  def getObjectListProp(param: String): Seq[Config] = Try(config.getConfigList(getParam(param)).asScala).getOrElse(Seq())

  def banner = Banners(config.getString("hbc.banner"))
}

object ConfigHelper extends ConfigHelper
