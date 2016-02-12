
import sbt._

/** All dependencies are TBD until end of PoC. All dependencies are tied to phases of rollout. */
object Library {

  //TODO play, spray, scalatra what have you, pick one. Akka http may still be too young/slow.
  val akkaActor      = "com.typesafe.akka"   %% "akka-actor"                   % Version.Akka
  val akkaRemote     = "com.typesafe.akka"   %% "akka-remote"                  % Version.Akka
  val akkaCluster    = "com.typesafe.akka"   %% "akka-cluster"                 % Version.Akka
  val akkaHttp       = "com.typesafe.akka"   %% "akka-http-core-experimental"  % Version.Akka
  val akkaStreams    = "com.typesafe.akka"   %% "akka-stream"                  % Version.Akka
  val config         = "com.typesafe"        %  "config"                       % Version.Config
  val eventuate      = "com.rbmhtechnology"  %% "eventuate"                    % Version.Eventuate
  val jodaTime       = "joda-time"           %  "joda-time"                    % Version.JodaT
  val jodaConvert    = "org.joda"            %  "joda-convert"                 % Version.JodaC
  val jsonCore       = "org.json4s"          %% "json4s-core"                  % Version.Json4s
  val jsonJackson    = "org.json4s"          %% "json4s-jackson"               % Version.Json4s
  val jsonNative     = "org.json4s"          %% "json4s-native"                % Version.Json4s
  val kafka          = "org.apache.kafka"    %% "kafka"                        % Version.Kafka excludeAll(Exclusions.forKafka:_*)
  val kafkaClients   = "org.apache.kafka"    %% "kafka-clients"                % Version.Kafka
  val logback        = "ch.qos.logback"      %  "logback-classic"              % Version.Logback
  val sLogging       = "com.typesafe.scala-logging" %% "scala-logging"         % Version.ScalaLogging
  val sparkStreamingKafka = "org.apache.spark" %% "spark-streaming-kafka"      % Version.Spark
  val sparkML        = "org.apache.spark" %% "spark-mllib"                     % Version.Spark
  val sparkCassandra = "com.datastax.spark"  %% "spark-cassandra-connector"    % Version.SparkCassandra

  object Test {
    val akkaTestkit      = "com.typesafe.akka" %% "akka-testkit"                 % Version.Akka
    val commonsIo        = "commons-io"        %  "commons-io"                   % "2.4"
    val multiNodeTestkit = "com.typesafe.akka" %% "akka-multi-node-testkit"      % Version.Akka
    val embeddedKafka    = "com.tuplejump"     %% "embedded-kafka"               % Version.EmbeddedKafka
    val scalaCheck       = "org.scalacheck"    %% "scalacheck"                   % Version.ScalaCheck
    val scalaTest        = "org.scalatest"     %% "scalatest"                    % Version.ScalaTest
  }

  object Examples {
    val rtbValidator   = "org.openrtb"         % "openrtb-validator"             % Version.RtbValidator
  }
}

object Dependencies {

  val logging = List(Library.logback, Library.sLogging)

  val time = List(Library.jodaTime, Library.jodaConvert)

  val json = List(Library.jsonCore, Library.jsonJackson, Library.jsonNative)

  lazy val testkit = {
    import Library._
    List(Test.akkaTestkit, Test.scalaCheck, Test.scalaTest)
      //.map(_ % "test,it")
  }

  lazy val cqrs = time ++ List(
    Library.config
  )

  lazy val core = cqrs ++ logging ++ List(
    Library.eventuate,//has akka-remote
    //until I get the testkit classpath in MultiJvm:
    Library.Test.scalaTest,
    Library.Test.multiNodeTestkit //Library.Test.commonsIo
  )

  // Library.kafkaClients, //issue with repo ATM. working out.
  lazy val cluster = core ++ logging ++ List(
    Library.kafka
  )

  lazy val topology = List(
   Library.akkaCluster
  )

  //sparse for the moment, adding more
  lazy val analytics = core ++ List(
    Library.sparkML, Library.sparkStreamingKafka
  )

  //TODO add play/spray
  lazy val api = core ++ logging ++ json

}

object Exclusions {

  lazy val forSpark = List.empty//TODO

  lazy val forCassandra = List.empty//TODO

  lazy val forEventuate = List (
    ExclusionRule("org.fusesource.leveldbjni")
  )

  lazy val forKafka = List(
    ExclusionRule("com.sun.jmx", "jmxri"),
    ExclusionRule("com.sun.jdmk", "jmxtools"),
    ExclusionRule("net.sf.jopt-simple", "jopt-simple"),
    ExclusionRule("org.slf4j", "slf4j-simple"),
    ExclusionRule("org.slf4j", "slf4j-log4j12")
  )
}


