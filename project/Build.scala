

import scala.language.postfixOps
import sbt.Keys._
import sbt._
import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys._

object Build extends sbt.Build {

  lazy val root = Project(
    id = "root",
    base = file("."),
    settings = Settings.parentSettings,
    aggregate = List(testkit, common, core, kafka, examples)
  )

  lazy val testkit = LibraryProject("testkit", Dependencies.testkit)

  lazy val common = LibraryProject("common", Dependencies.common)

  lazy val core = AssembledProject("core", Dependencies.core).configs(MultiJvm)

  lazy val kafka = AssembledProject("kafka", Dependencies.kafka)

  lazy val examples = AssembledProject("examples", Dependencies.examples)
    .dependsOn(kafka).dependsOn(core).settings(publishArtifact := false)

  def AssembledProject(name: String, deps: Seq[ModuleID]): Project =
    Project(
      id = name,
      base = file("continuum-" + name),
      dependencies = List(
        common % "compile;runtime->runtime;test->test;it->it,test;it;provided->provided",
        testkit % "test->test;it->it,test;"
      ),
      settings = Settings.common ++ Settings.multiJvmSettings ++ Seq(
        libraryDependencies ++= deps
      )
    ).enablePlugins(AutomateHeaderPlugin) configs IntegrationTest

  def LibraryProject(name: String, dependencies: Seq[ModuleID]): Project =
    Project(
      id = name,
      base = file("continuum-" + name),
      settings = Settings.common ++ Seq(
        libraryDependencies ++= dependencies
      )
    ).enablePlugins(AutomateHeaderPlugin) configs IntegrationTest

}
