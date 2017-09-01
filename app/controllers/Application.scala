package controllers

import javax.inject._

import ch.qos.logback.classic.Level
import com.iheart.playSwagger.SwaggerSpecGenerator
import helpers.ControllerPayload
import models.Toggle
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import services.LocalizationService
import webservices.toggles.TogglesClient

import scala.concurrent.Future

class Application @Inject() (
    togglesClient:       TogglesClient,
    localizationService: LocalizationService
) extends Controller with ControllerPayload {

  def index = Action.async {
    implicit request =>
      val response = "account-service is up and running!"
      Future.successful(writeResponseGet(response))
  }

  def changeLogLevel(levelString: String) = Action.async {
    implicit request =>
      Logger.debug("account-service change log level called")
      val level = Level.toLevel(levelString)
      Logger.underlying().asInstanceOf[ch.qos.logback.classic.Logger].setLevel(level)
      val response = s"Log level changed to $level"
      Future.successful(writeResponseGet(response))
  }

  def clearToggles(name: Option[String]) = Action.async {
    implicit request =>
      togglesClient.clearCache(name)
      Future.successful(writeResponseGet("done!"))
  }

  def toggles(name: Option[String]) = Action.async {
    implicit request =>

      val toggles: Future[Seq[Toggle]] = name match {
        case Some(toggleName) => togglesClient.getToggle(toggleName).map(t => Seq(t))
        case None             => togglesClient.getToggles()
      }
      toggles map { r => writeResponseGet(r) }
  }

  def clear() = Action.async {
    implicit request =>
      localizationService.clear()
      Future.successful(writeResponseGet("cache cleared!"))
  }

  implicit val cl = getClass.getClassLoader
  private lazy val generator = SwaggerSpecGenerator("scala.collection.mutable", "models")

  def getApiDocs() = Action.async { implicit request =>
    Future.fromTry(generator.generate()).map(Ok(_))
  }

  def renderSwaggerUi = Action(Redirect("/v1/account-service/api-docs/ui/index.html?url=/v1/account-service/api-docs"))

}
