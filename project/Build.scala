
import scala.language.postfixOps
import sbt.Keys._
import sbt._
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys._

object Build extends sbt.Build {

  lazy val root = Project(
    id = "root",
    base = file("."),
    settings = Settings.parentSettings,
    aggregate = List(testkit, cqrs, core, cluster, examples)// later: analytics, api)
  )

  lazy val testkit = LibraryProject("testkit", Dependencies.testkit)

  lazy val cqrs = LibraryProject("cqrs", Dependencies.cqrs)

  lazy val core = AssembledProject("core", Dependencies.core)

  lazy val cluster = AssembledProject("cluster", Dependencies.cluster)

  lazy val topology = AssembledProject("topology", Dependencies.topology)

  lazy val analytics = AssembledProject("analytics", Dependencies.analytics)

  lazy val api = AssembledProject("api", Dependencies.api)

  lazy val examples = AssembledProject("examples", Dependencies.examples)
    .dependsOn(cluster).dependsOn(core)

  def AssembledProject(name: String, deps: Seq[ModuleID]): Project =
    Project(
      id = name,
      base = file("continuum-" + name),
      dependencies = List(
        cqrs % "compile;runtime->runtime;test->test;it->it,test;it;provided->provided",
        testkit % "test->test;it->it,test;"
      ),
      settings = Settings.common ++ Settings.multiJvmSettings ++ Seq(
        libraryDependencies ++= deps
      )) configs IntegrationTest configs MultiJvm

  def LibraryProject(name: String, dependencies: Seq[ModuleID]): Project =
    Project(
      id = name,
      base = file("continuum-" + name),
      settings = Settings.common ++ Settings.multiJvmSettings ++ Seq(
        libraryDependencies ++= dependencies
      )) configs IntegrationTest configs MultiJvm

}
