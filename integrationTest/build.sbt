name := "account-service-integration-tests"

version := "0.1"

scalaVersion := "2.11.1"
libraryDependencies ++= Seq(
  "com.typesafe.play" % "play-ws_2.11" % "2.5.13",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "com.jayway.restassured" % "rest-assured" % "2.8.0" % "test",
  "io.rest-assured" % "scala-support" % "3.0.2" % "test"
)
