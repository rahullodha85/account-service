package helpers

import javax.inject.Inject

import constants.Constants
import webservices.toggles.TogglesClient

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

class TogglesHelper @Inject() (
    togglesClient: TogglesClient
) {

  def getWaitForMoreToggleState: Future[Boolean] = {
    togglesClient.getToggleState(Constants.TOGGLE_WAIT_FOR_MORE_NUMBER) recoverWith {
      case _ => Future.successful(false)
    }
  }

  def getFavoritesToggleState: Future[Boolean] = {
    togglesClient.getToggleState(Constants.IS_FAVORITE_ENABLED) recoverWith {
      case _ => Future.successful(false)
    }
  }

  def migratingSaksFirst: Future[Boolean] = {
    togglesClient.getToggleState(Constants.MIGRATING_SAKS_FIRST) recoverWith {
      case _ => Future.successful(true)
    }
  }

  def saksFirstSummaryEnabled: Future[Boolean] = {
    togglesClient.getToggleState(Constants.SAKS_FIRST_SUMMARY_ENABLED) recoverWith {
      case _ => Future.successful(false)
    }
  }

  def giltEmailSubscription: Future[Boolean] = {
    togglesClient.getToggleState(Constants.GILT_EMAIL_SUBSCRIPTION) recoverWith {
      case _ => Future.successful(true)
    }
  }

  def saksFirstPageEnabled: Future[Boolean] = {
    togglesClient.getToggleState(Constants.SAKS_FIRST_PAGE) recoverWith {
      case _ => Future.successful(false)
    }
  }

  def disableRedemption: Future[Boolean] = {
    togglesClient.getToggleState(Constants.DISABLE_SAKS_FIRST_REDEMPTION) recoverWith {
      case _ => Future.successful(false)
    }
  }
  def beautyEnabled: Future[Boolean] = {
    togglesClient.getToggleState(Constants.BEAUTY_ENABLED) recoverWith {
      case _ => Future.successful(false)
    }
  }
}
