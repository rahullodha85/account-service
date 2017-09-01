import play.core.PlayVersion
import sbt._

/* List the dependencies that are common across all microservices
 * DO NOT list dependencies that are specific to a microservice. Use 'ServiceDependencies' instead. */
object CommonDependencies {

  val scalaTestVersion = "2.2.6"
  val scalaTestPlusVersion = "1.5.1"
  val scalaCheckVersion = "1.12.5"
  val playWSVersion = PlayVersion.current
  val playMockWSVersion = "2.5.0"
  val sprayVersion = "1.3.3"
  val gsonVersion = "1.7.1"
  val mockitoVersion = "1.10.19"

  val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
  val scalaTestPlus = "org.scalatestplus.play" %% "scalatestplus-play"  % scalaTestPlusVersion % "test"
  val scalacheck = "org.scalacheck" %% "scalacheck" % scalaCheckVersion % "test"
  val playWS = "com.typesafe.play" %% "play-ws" % playWSVersion
  val sprayCaching = "io.spray" %% "spray-caching" % sprayVersion
  val playSwagger = "com.iheart" %% "play-swagger" % "0.3.2-PLAY2.5"
  val swaggerUi = "org.webjars" % "swagger-ui" % "2.1.4"
  val mockito = "org.mockito" % "mockito-core" % mockitoVersion % "test"
  val playMockWS = "de.leanovate.play-mockws" %% "play-mockws" % playMockWSVersion % "test"

  val commonDependencies : Seq[ModuleID] =
    Seq(
      playWS,
      scalaTest,
      scalaTestPlus,
      scalacheck,
      sprayCaching,
      playSwagger,
      swaggerUi,
      playMockWS,
      mockito
    )
}