package com.tuplejump.continuum

import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils

import scala.util.control.NoStackTrace

trait TestHelper {

  val failure = new Exception("boom") with NoStackTrace

  val testConfig =  ConfigFactory.parseString(
    """
      |akka.remote.netty.tcp.port=0
      |akka.remote.enabled-transports = ["akka.remote.netty.tcp"]
    """.stripMargin)

}

trait TestIo {
  import java.io.{ File => JFile }
  import java.nio.file.{ Paths, Path }
  import java.util.UUID

  private val shutdownDeletePaths = new scala.collection.mutable.HashSet[String]()

  /** Creates tmp dirs that automatically get deleted on shutdown. */
  def createTempDir(tmpName: String = UUID.randomUUID.toString): JFile = {
    val tmp = Paths.get(sys.props("java.io.tmpdir"))
    val name: Path = tmp.getFileSystem.getPath(tmpName)
    require(Option(name.getParent).isEmpty, "Invalid prefix or suffix for tmp dir.")

    val dir = tmp.resolve(name).toFile
    registerShutdownDeleteDir(dir)

    Runtime.getRuntime.addShutdownHook(new Thread("delete temp dir " + dir) {
      override def run() {
        if (!hasRootAsShutdownDeleteDir(dir)) deleteRecursively(dir)
      }
    })
    dir
  }

  protected def deleteRecursively(file: JFile): Unit =
    for {
      file <- Option(file)
    } FileUtils.deleteDirectory(file)

  protected def registerShutdownDeleteDir(file: JFile) {
    shutdownDeletePaths.synchronized {
      shutdownDeletePaths += file.getAbsolutePath
    }
  }

  private def hasRootAsShutdownDeleteDir(file: JFile): Boolean = {
    val absolutePath = file.getAbsolutePath
    shutdownDeletePaths.synchronized {
      shutdownDeletePaths.exists { path =>
        !absolutePath.equals(path) && absolutePath.startsWith(path)
      }
    }
  }
}