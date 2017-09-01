package services

import akka.actor.Actor
import play.Logger

import scala.concurrent.duration._
import scala.language.postfixOps

object InstructionMessage extends Enumeration {
  val Start, Clear = Value
}

class CountriesCacheActor(localizationService: LocalizationService) extends Actor {
  import play.api.libs.concurrent.Execution.Implicits._

  def receive = {
    case InstructionMessage.Start =>
      localizationService.cachedCountries("countries")
        .map { countries =>
          Logger.info(s"${countries.size} countries retrieved and cached for future calls.")
          context stop self
        } recover {
          case e: Throwable =>
            Logger.error(s"Failed to retrieved and cached countries, Retrying in 60 seconds ${e.getMessage}", e)
            context.system.scheduler.scheduleOnce(60 seconds, self, InstructionMessage.Start)
        }
    case InstructionMessage.Clear =>
      localizationService.clear()
  }
}
