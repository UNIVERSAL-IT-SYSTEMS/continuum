

import scala.language.postfixOps
import sbt.Keys._
import sbt._
import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
import de.heikoseeberger.sbtheader.HeaderPlugin
import de.heikoseeberger.sbtheader.license.Apache2_0
import wartremover.WartRemover.autoImport._
import org.scalastyle.sbt.ScalastylePlugin._
import scoverage.ScoverageKeys

object Settings extends sbt.Build {

  scalaVersion := Version.Scala

  lazy val buildSettings = Seq(

    name := baseDirectory.value.getName,

    organization := "com.tuplejump",

    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),

    resolvers += "confluent" at "http://packages.confluent.io/maven/",

    pomExtra :=
        <developers>
          <developer>
            <id>helena</id>
            <name>Helena Edelson</name>
            <url>https://twitter.com/helenaedelson</url>
          </developer>
        </developers>,

    HeaderPlugin.autoImport.headers := Map(
      "scala" -> Apache2_0("2016", "Tuplejump"),
      "conf"  -> Apache2_0("2016", "Tuplejump", "#")
    )
  )

  lazy val encoding = Seq("-encoding", "UTF-8")

  lazy val srcDirs = List(managedSourceDirectories, unmanagedSourceDirectories)

  lazy val configs = List(Compile, Test, MultiJvm)

  lazy val dirs = for {
    dir  <- srcDirs
    conf <- configs
  } yield dir in conf := (scalaSource in conf).value :: Nil

  lazy val parentSettings = buildSettings ++ dirs

  lazy val common = buildSettings ++ dirs ++ testSettings ++ wartremoverSettings ++ styleSettings ++ Seq(

    scalaVersion := Version.Scala,

    cancelable in Global := true,

    logBuffered in Compile := false,
    logBuffered in Test := false,

    outputStrategy := Some(StdoutOutput),

    ScoverageKeys.coverageHighlighting := true,

    aggregate in update := false,

    updateOptions := updateOptions.value.withCachedResolution(true),

    incOptions := incOptions.value.withNameHashing(true),

    scalacOptions ++= encoding ++ fatalWarnings ++ Seq(
      "-deprecation",
      "-feature",
      "-language:_",
      "-unchecked",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-unused-import"
    ),

    javacOptions ++= encoding ++ Seq(
      "-source", Version.JavaBinary,
      "-target", Version.JavaBinary,
      "-Xmx1G",
      "-Xlint:unchecked",
      "-Xlint:deprecation"
    ),

    autoCompilerPlugins := true,
    autoAPIMappings := true,

    ivyScala := ivyScala.value map {
      _.copy(overrideScalaVersion = true)
    },

    ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet,

    evictionWarningOptions in update := EvictionWarningOptions.default
      .withWarnTransitiveEvictions(false)
      .withWarnDirectEvictions(false)
      .withWarnScalaVersionEviction(false),

    parallelExecution in ThisBuild := false,

    parallelExecution in Global := false,

    publishMavenStyle := false

    /* exportJars := true,*/
  )

  val fatalWarnings = Version.JavaBinary match {
    case "1.8" => Seq("-Xfatal-warnings")
    case "1.7" => Seq.empty
  }

  val warts = Warts.allBut(
    Wart.Any, //actor
    Wart.Throw, //TODO fix
    Wart.NonUnitStatements,
    Wart.Nothing,
    Wart.Var,
    Wart.ToString)

  val wartremoverSettings = Seq(
    wartremoverExcluded ++= {
      val path = baseDirectory.value / "src" / "main" / "scala" / "com" / "tuplejump" / "continuum"
      Seq("Converters.scala","Kafka.scala","ContinuumExtension.scala","Continuum.scala")
        .map(path / _)
     },

    wartremoverErrors in (Compile, compile) := warts,
    wartremoverErrors in (Test, compile) := warts,
    wartremoverErrors in (IntegrationTest, compile) := warts
  )

  val compileScalastyle = taskKey[Unit]("compileScalastyle")

  val testScalastyle = taskKey[Unit]("testScalastyle")

  lazy val styleSettings = Seq(
    testScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Test).toTask("").value,
    scalastyleFailOnError := true,
    compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value
  )

  val testConfigs = inConfig(Test)(Defaults.testTasks) ++ inConfig(IntegrationTest)(Defaults.itSettings)

  val testOptionSettings = Seq(
    Tests.Argument(TestFrameworks.ScalaTest, "-oDF")
  )

  lazy val testSettings = testConfigs ++ Seq(
    parallelExecution in Test := false,
    parallelExecution in IntegrationTest := false,
    testOptions in Test ++= testOptionSettings,
    testOptions in IntegrationTest ++= testOptionSettings,
    fork in Test := true,
    fork in IntegrationTest := true,

    managedClasspath in IntegrationTest <<= Classpaths.concat(managedClasspath in IntegrationTest, exportedProducts in Test),
    (compile in IntegrationTest) <<= (compile in Test, compile in IntegrationTest) map { (_, c) => c },
    (internalDependencyClasspath in IntegrationTest) <<= Classpaths.concat(internalDependencyClasspath in IntegrationTest, exportedProducts in Test)
  )

  lazy val multiJvmSettings = SbtMultiJvm.multiJvmSettings ++ Seq(
    compile in MultiJvm <<= (compile in MultiJvm) triggeredBy (compile in Test),
    parallelExecution in Test := false,
    executeTests in Test <<=
      ((executeTests in Test), (executeTests in MultiJvm)) map {
        case ((testResults), (multiJvmResults)) =>
          val overall =
            if (testResults.overall.id < multiJvmResults.overall.id) multiJvmResults.overall
            else testResults.overall
          Tests.Output(overall,
            testResults.events ++ multiJvmResults.events,
            testResults.summaries ++ multiJvmResults.summaries)
      }
  )
}
