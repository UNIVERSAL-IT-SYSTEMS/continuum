package examples

import java.io.{File => JFile}

import kafka.common.{TopicAlreadyMarkedForDeletionException, TopicExistsException}

import scala.util.Try
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils
import com.tuplejump.continuum.Assertions

object Node {

  private val dir = tempDirForConfig("examples")

  def create(port: Int): ActorSystem = {

    val config = ConfigFactory.parseString(
        s"""
           |akka.remote.netty.tcp.port=$port
           |kafka.log.dir="${dir.getAbsolutePath}"
           |""".stripMargin).withFallback(ConfigFactory.load())

    val as = ActorSystem("Examples", config)
    Runtime.getRuntime.addShutdownHook(new Thread(s"Shutting down.") {
      override def run(): Unit = {
        as.terminate()
        import scala.concurrent.duration._
        scala.concurrent.Await.result(as.whenTerminated, 4.seconds)

         Try(FileUtils.deleteDirectory(dir))
      }
    })
    as
  }

  //import akka.event.Logging
  //lazy val log = Logging(system, system.name)

  private def tempDirForConfig(name: String): JFile = {
    val dir = new JFile(sys.props("java.io.tmpdir") + JFile.separator + name)
    //require(dir.exists && dir.canExecute)
    //dir.deleteOnExit()
    dir
  }
}

/** Handles example app lifecycle work: setup, shutdown... */
object NodeLifecycleKafka extends Assertions {

  import kafka.admin.AdminUtils
  import kafka.utils.ZkUtils
  import com.tuplejump.continuum.KafkaSettings

  private[examples] def simulation(settings: KafkaSettings, topics: Set[String]): Unit = {
    val zk = ZkUtils(settings.KafkaZookeeperConnect.mkString(","), 6000, 6000, false)

    topics foreach { topic =>
      try {
        AdminUtils.createTopic(zk, topic, 1, 1)
        await(5000,300)(AdminUtils.topicExists(zk, topic))
      } catch { case e: TopicExistsException => /* ignore */ }
    }

    Runtime.getRuntime.addShutdownHook(new Thread(s"Cleanup ${getClass.getName}") {
      override def run(): Unit = {
        for (topic <- topics) {
          try AdminUtils.deleteTopic(zk, topic)
          catch { case e: TopicAlreadyMarkedForDeletionException => /* ignore for now */ }
        }
        zk.close()
      }
    })
  }
}
