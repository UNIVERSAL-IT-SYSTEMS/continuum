object Version {

  final val Akka             = "2.4.2-RC2"//Eventuate uses 2.4.1
  final val Config           = "1.3.0"
  final val EmbeddedKafka    = "0.0.4"//temporary - for prototyping quickly
  final val Eventuate        = "0.5"//akka val akkaVersion = "2.4.1"
  final val Json4s           = "3.3.0"
  final val Kafka            = "0.9.0.0"
  final val Logback          = "1.0.7"
  final val JavaVersion      = scala.util.Properties.javaVersion
  final val JavaBinary       = JavaVersion.dropRight(5)
  final val JodaC            = "1.6"
  final val JodaT            = "2.5"
  final val Phantom          = "1.12.2"
  final val Play             = "2.4.4"//compat with Akka 2.3.13
  final val RtbValidator     = "2.3.1"
  final val Scala            = "2.11.7"
  final val ScalaCheck       = "1.12.5"
  final val ScalaLogging     = "3.1.0"
  final val ScalaTest        = "2.2.5"
  final val Slf4j            = "1.7.13"
  final val Spark            = "1.5.2"    //1.6.0
  final val SparkCassandra   = "1.5.0-M3"//uses spark 1.5.1 :(

}
