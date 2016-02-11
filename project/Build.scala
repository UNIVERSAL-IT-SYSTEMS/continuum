
import scala.language.postfixOps
import sbt.Keys._
import sbt._
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys._

object Build extends sbt.Build {

  type CPD = ClasspathDep[ProjectReference]

  lazy val root = Project(
    id = "root",
    base = file("."),
    settings = Settings.parentSettings,
    aggregate = List(testkit, core, continuum, cluster, analytics, api)
  )

  lazy val testkit = LibraryProject("continuum-testkit", Dependencies.testkit)

  lazy val core = LibraryProject("continuum-core", Dependencies.core)

  lazy val continuum = AssembledProject("continuum", Dependencies.continuum, List(core))

  lazy val cluster = AssembledProject("continuum-cluster", Dependencies.cluster, List(core))

  lazy val topology = AssembledProject("continuum-topology", Dependencies.topology)

  lazy val analytics = AssembledProject("continuum-analytics", Dependencies.analytics)

  lazy val api = AssembledProject("continuum-api", Dependencies.api)

  def AssembledProject(name: String, dependencies: Seq[ModuleID], modules: Seq[CPD] = Seq.empty): Project =
    Project(
      id = if(name.contains("-")) name.replace("continuum-", "") else name,
      base = file(name),
      dependencies = modules ++ List(testkit % "test->test;it->it,test"),
      settings = Settings.common ++ Settings.multiJvmSettings ++ Seq(
        libraryDependencies ++= dependencies
      )) configs IntegrationTest configs MultiJvm


  def LibraryProject(name: String, dependencies: Seq[ModuleID], modules: Seq[CPD] = Seq.empty): Project =
    Project(
      id = name,
      base = file(name),
      settings = Settings.common ++ Settings.multiJvmSettings ++ Seq(
        libraryDependencies ++= dependencies
      )) configs IntegrationTest configs MultiJvm

}
