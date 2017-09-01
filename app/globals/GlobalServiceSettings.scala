package globals

import javax.inject.{ Inject, Singleton }

import akka.actor.Props
import play.api.inject.ApplicationLifecycle
import services.{ CountriesCacheActor, InstructionMessage, LocalizationService }

@Singleton
class GlobalServiceSettings @Inject() (applicationLifecycle: ApplicationLifecycle, localizationService: LocalizationService) {

  val locationsCacheActor = actorSystem.actorOf(Props(classOf[CountriesCacheActor], localizationService), "countriesCacheActor")
  locationsCacheActor ! InstructionMessage.Start

  applicationLifecycle.addStopHook { () =>
    actorSystem.terminate()
  }
}
