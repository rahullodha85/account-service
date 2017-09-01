import CommonDependencies._
import ServiceDependencies._
import net.virtualvoid.sbt.graph.Plugin._

import scalariform.formatter.preferences._

name := """account-service"""

version := "0.1"

scapegoatVersion := "1.1.0"

envVars := Map("PRIVATE_IP" -> "someServer", "HBC_BANNER" -> "o5a", "DEV_MODE" -> "false", "MOBILE_HOST"->"mobile-devslot2.saksdirect.com", "WEBSITE_HOST"-> "web1-devslot2.saksdirect.com")

val defaultSettings: Seq[Setting[_]] = Seq(
      scalaVersion  := "2.11.7",
      scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature","-Ywarn-unused-import"),
      libraryDependencies ++= commonDependencies,
      routesGenerator := InjectedRoutesGenerator,
      parallelExecution in Test := false
      ) ++ graphSettings
      

lazy val root = (project in file("."))
    .settings(defaultSettings: _*)
    .settings(
      libraryDependencies ++= serviceDependencies
    )
    .enablePlugins(PlayScala)

resolvers ++= Seq("Saks Artifactory - Ext Release Local" at "http://repo.saksdirect.com:8081/artifactory/ext-release-local",
	"Saks Artifactory - Libs Release Local" at "http://repo.saksdirect.com:8081/artifactory/libs-release-local",
	"Saks Artifactory - Libs Release" at "http://repo.saksdirect.com:8081/artifactory/libs-release",
  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  "jitpack" at "https://jitpack.io",
  Resolver.jcenterRepo
)
		   
lazy val buildAll = TaskKey[Unit]("build-all", "Compiles and runs all tests")
lazy val buildZip = TaskKey[Unit]("build-zip","Compiles, tests, and publishes a zip file with the new code.")
lazy val buildTestArtifact = TaskKey[Unit]("build-test","Compiles, tests, and publishes a zip file with the integration tests.")
lazy val preCommit = TaskKey[Unit]("pre-commit", "Compiles, tests, zips code, and then refreshes docker container.")

buildAll <<= Seq(clean, compile in Compile, compile in Test, test in Test).dependOn

buildTestArtifact <<= (baseDirectory, Keys.name) map { (bd: File, artifactName: String) =>
  val zipfile = file("target/universal/" + artifactName + "-test-artifact.zip")
  def entries(f: File):List[File] = f :: (if (f.isDirectory) IO.listFiles(f).toList.flatMap(entries(_)) else Nil)
  IO.zip(entries(bd).map(d => (d, d.getAbsolutePath.substring(bd.getParent.length))), zipfile)
}

buildZip <<= ((packageBin in Universal) map { out =>
  println("Copying Zip file to docker directory.")
  IO.move(out, file(out.getParent + "/../../docker/" + out.getName))
}).dependsOn(buildAll)

dist <<= dist.dependsOn(buildTestArtifact)

preCommit := {"./refresh-service.sh"!}

preCommit <<= preCommit.dependsOn(buildZip)

scalariformSettings

ScalariformKeys.preferences := FormattingPreferences()
  .setPreference( AlignParameters, true )
  .setPreference( AlignSingleLineCaseStatements, true )
  .setPreference( DoubleIndentClassDeclaration, true )

scoverage.ScoverageKeys.coverageEnabled.in(ThisBuild ,Test, test) := true

scoverage.ScoverageKeys.coverageMinimum := 50

scoverage.ScoverageKeys.coverageFailOnMinimum := false

scoverage.ScoverageKeys.coverageHighlighting := {
    if(scalaBinaryVersion.value == "2.11") true
    else false
}

