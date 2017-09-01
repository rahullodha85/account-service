package controllers

import javax.inject._

import play.api._
import play.api.mvc._
import play.api.libs.json._
import helpers.AdminHelper._
import helpers.ControllerPayload

import scala.concurrent.Future

class Admin @Inject() () extends ControllerPayload {

  def ping = Action.async {
    implicit request =>
      Logger.debug("ping")
      Future.successful(writeResponseGet("pong"))
  }

  def jvmstats = Action.async {
    implicit request =>
      Logger.debug("jvmstats")
      Future.successful(writeResponseGet(Json.toJson(extractJvmStats())))
  }
}
