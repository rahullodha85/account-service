import sbt._

/* List the dependencies specific to the service here */
object ServiceDependencies {
  val casbahVersion = "2.8.1"
  val mongoDriverVersion = "2.13.1"
  val salatVersion = "1.9.9"
  val reactiveMongoVersion = "0.11.1.play23"
  val akkaVersion = "2.3.9"
  val logstashLogbackEncoderVersion = "4.6"

  val serviceDependencies : Seq[ModuleID] = Seq(
    "org.mongodb" %% "casbah" % casbahVersion,
    "org.mongodb" % "mongo-java-driver" % mongoDriverVersion,
    "com.novus" %% "salat" % salatVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "net.logstash.logback" % "logstash-logback-encoder" % logstashLogbackEncoderVersion
  )
}
