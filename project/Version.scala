object Version {

  final val Scala            = "2.11.7"

  //akka-stream on 2.4.2-RC3, eventuate on 2.4.1
  //akka 2.3.14 is the last that config supports jdk7 (config 1.2.1)
  final val Akka             = "2.4.1"
  final val Eventuate        = "0.5"
  final val Json4s           = "3.3.0"
  final val Kafka            = "0.9.0.0"
  final val Logback          = "1.0.7"
  final val JavaVersion      = scala.util.Properties.javaVersion
  final val JavaBinary       = JavaVersion.dropRight(5)
  final val JodaC            = "1.8.1"
  final val JodaT            = "2.9.2"
  final val ScalaLogging     = "3.1.0"
  final val Slf4j            = "1.7.13"
  final val CommonsIo        = "2.4"
  final val ScalaCheck       = "1.12.5"
  final val ScalaTest        = "2.2.5"
}
