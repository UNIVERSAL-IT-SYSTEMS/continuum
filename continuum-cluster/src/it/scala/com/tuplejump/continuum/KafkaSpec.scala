package com.tuplejump.continuum

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory

object KafkaSpec extends TestIo with Assertions {

  lazy val config = ConfigFactory.parseString(
    s"""
       |kafka.log.dir=${createTempDir("kafka-spec")}
    """.stripMargin)

  lazy val settings = new KafkaSettings(config)

  def setup(): Unit = {
    //    require(!settings.hasLocal, "A local kafka and zookeeper server are running. Please stop them while running tests.")
    import kafka.admin.AdminUtils
    import kafka.utils.ZkUtils

    val topic = "topic"
    val zkutils = ZkUtils(settings.KafkaZookeeperConnect.mkString(","), 6000, 6000, false)
    AdminUtils.createTopic(zkutils, "topic", 1, 1)
    await(5000,300)(AdminUtils.topicExists(zkutils, topic))
    zkutils.close()

    AdminUtils.deleteTopic(zkutils, topic)
/*
    import scala.collection.JavaConverters._
    import kafka.server.KafkaConfig

    val kafkaConfig = Try(KafkaConfig(settings.brokerConfig.asJava)).toOption

    if (kafkaConfig.isDefined) {
      val zkUtils = ZkUtils(kafkaConfig.get.zkConnect,
        kafkaConfig.get.zkSessionTimeoutMs,
        kafkaConfig.get.zkConnectionTimeoutMs,
        isZkSecurityEnabled = false)

      AdminUtils.createTopic(zkutils, topic, 1, 1) //TODO add topic config
      await(5000,300)(AdminUtils.topicExists(zkutils, topic))
      zkUtils.close()

    } else {

      /*val embedded = new EmbeddedKafka()
      embedded.start()
      await(3000,500)(embedded.isRunning)
      embedded.createTopic("topic", 1, 1)
      embedded.shutdown()*/
    }*/
  }
}

class KafkaSpec extends TestKit(ActorSystem("KafkaSpec", KafkaSpec.config)) with AbstractSpec {

  "Kafka Extension" must {
    "work" in {
      val settings = KafkaSpec.settings
      //require(!settings.hasLocal, "A local kafka and zookeeper server are running. Please stop them while running tests.")
      KafkaSpec.setup()
      //val kafka = Kafka(system)
    }
  }
}
