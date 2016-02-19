
import sbt._

object Library {

  val akkaActor        = "com.typesafe.akka"          %% "akka-actor"              % Version.Akka
  val akkaRemote       = "com.typesafe.akka"          %% "akka-remote"             % Version.Akka
  val eventuate        = "com.rbmhtechnology"         %% "eventuate"               % Version.Eventuate //has akka-remote
  val jodaTime         = "joda-time"                  %  "joda-time"               % Version.JodaT
  val jodaConvert      = "org.joda"                   %  "joda-convert"            % Version.JodaC
  val kafkaClients     = "org.apache.kafka"           % "kafka-clients"            % Version.Kafka
  val logback          = "ch.qos.logback"             %  "logback-classic"         % Version.Logback
  val sLogging         = "com.typesafe.scala-logging" %% "scala-logging"           % Version.ScalaLogging

  val akkaTestkit      = "com.typesafe.akka"          %% "akka-testkit"            % Version.Akka          % "test,it"
  val commonsIo        = "commons-io"                 %  "commons-io"              % Version.CommonsIo     % "test,it"
  val multiNodeTestkit = "com.typesafe.akka"          %% "akka-multi-node-testkit" % Version.Akka          % "test,it"
  val scalaCheck       = "org.scalacheck"             %% "scalacheck"              % Version.ScalaCheck    % "test,it"
  val scalaTest        = "org.scalatest"              %% "scalatest"               % Version.ScalaTest     % "test,it"
  /* test and examples/run */
  val kafka            = "org.apache.kafka"           %% "kafka"                   % Version.Kafka         excludeAll(Exclusions.forKafka:_*)

}

object Dependencies {

  private val logging = List(Library.logback, Library.sLogging)

  private val time = List(Library.jodaTime, Library.jodaConvert)

  lazy val testkit = List(
    Library.commonsIo,
    Library.scalaCheck,
    Library.scalaTest
  )

  lazy val common = time ++ logging

  lazy val core = common ++ List(
    Library.eventuate,
    Library.multiNodeTestkit
  )

  lazy val kafka = common ++ List(
    Library.akkaActor,
    Library.akkaRemote,
    Library.kafkaClients,
    Library.akkaTestkit,
    Library.multiNodeTestkit,
    Library.kafka % "test,it"
  )

  lazy val examples = List(
    Library.kafka
  )

}

object Exclusions {

  lazy val forKafka = List(
    ExclusionRule("com.sun.jmx", "jmxri"),
    ExclusionRule("com.sun.jdmk", "jmxtools"),
    ExclusionRule("net.sf.jopt-simple", "jopt-simple"),
    ExclusionRule("org.slf4j", "slf4j-simple"),
    ExclusionRule("org.slf4j", "slf4j-log4j12")
  )
}

object NotYet {

  /* Not using yet
  val akkaCluster    = "com.typesafe.akka"   %% "akka-cluster"                 % Version.Akka
  val akkaHttp       = "com.typesafe.akka"   %% "akka-http-core-experimental"  % Version.Akka
  val akkaStreams    = "com.typesafe.akka"   %% "akka-stream"                  % Version.Akka
  val config         = "com.typesafe"        %  "config"                       % Version.Config
  val jsonCore       = "org.json4s"          %% "json4s-core"                  % Version.Json4s
  val jsonJackson    = "org.json4s"          %% "json4s-jackson"               % Version.Json4s
  val jsonNative     = "org.json4s"          %% "json4s-native"                % Version.Json4s
  val sparkStreamingKafka = "org.apache.spark" %% "spark-streaming-kafka"      % Version.Spark
  val sparkML        = "org.apache.spark" %% "spark-mllib"                     % Version.Spark
  object Examples {
    val rtbValidator   = "org.openrtb"         % "openrtb-validator"             % Version.RtbValidator
  } */


  //List(Library.jsonCore, Library.jsonJackson, Library.jsonNative)
  //List(Library.akkaCluster, Library.Test.akkaTestkit)
  //core ++ List(Library.sparkML, Library.sparkStreamingKafka)
  //core ++ logging ++ json

}

