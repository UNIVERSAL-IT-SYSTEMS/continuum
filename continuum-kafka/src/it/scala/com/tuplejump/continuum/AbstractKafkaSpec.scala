package com.tuplejump.continuum

import scala.concurrent.duration._
import scala.util.Try
import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.typesafe.config.{Config, ConfigFactory}
import com.tuplejump.embedded.kafka.EmbeddedKafka

/** Uses EmbeddedKafka. */
abstract class AbstractKafkaSpec(val topic: String, conf: Config)
  extends AbstractSpec with Assertions with Logging {

  def this(topic: String) =
    this(topic, ConfigFactory.load())

  protected val duration = 5.seconds

  protected val initialDelay = 2.second

  protected val embedded = new EmbeddedKafka()

  protected val config = ConfigFactory.parseString(
    s"""
       |continuum.kafka.log.dir="${embedded.dataDir.getAbsolutePath}"
       |akka.remote.netty.tcp.port = 0
     """.stripMargin)
    .withFallback(conf)

  implicit val system: ActorSystem = ActorSystem(getClass.getSimpleName, config)

  protected val testkit = new TestKit(system)

  override def beforeAll(): Unit = {
    embedded.isRunning should be(false)
    try embedded.start() catch { case e: java.net.BindException =>
        logger.error("You may have a local zookeeper or kafka instance running. Shut those down and re-run.", e)
    }
    testkit.awaitCond(embedded.isRunning, duration)

    embedded.createTopic(topic, 1, 1)
  }

  override def afterAll(): Unit = {
    logger.info("IT test completed, shutting down...")
    Try {
      testkit shutdown system
      embedded.shutdown()
      testkit.awaitCond(!embedded.isRunning, duration)
    }
  }
}