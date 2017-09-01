package modules

import com.google.inject.AbstractModule
import globals.GlobalServiceSettings
import play.api.{ Configuration, Environment }

class GlobalServiceSettingsProvider(
  environment:   Environment,
  configuration: Configuration
)
    extends AbstractModule {
  override def configure() {
    bind(classOf[GlobalServiceSettings]).asEagerSingleton()
  }
}