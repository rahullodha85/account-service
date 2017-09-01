package controllers

import javax.inject.Inject

import helpers.{ AccountHelper, ControllerPayload, RequestHelper, TogglesHelper }
import messages.HBCMessagesApi
import play.api.i18n.I18nSupport
import play.api.mvc.Controller
import play.api.mvc._
import models.website._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

class I18nController @Inject() (
    accountHelper:   AccountHelper,
    togglesHelper:   TogglesHelper,
    val messagesApi: HBCMessagesApi
) extends Controller with I18nSupport with ControllerPayload with RequestHelper {

  def getMessages = Action.async { implicit request =>
    for {
      addFavoritesTab <- togglesHelper.getFavoritesToggleState
      addSaksFirstPage <- togglesHelper.getFavoritesToggleState
    } yield writeResponseGet(Message(accountHelper.getHeader(addFavoritesTab, addSaksFirstPage)))
  }
}
