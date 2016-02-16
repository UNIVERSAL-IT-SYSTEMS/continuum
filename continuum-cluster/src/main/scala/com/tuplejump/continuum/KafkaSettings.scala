package com.tuplejump.continuum

import java.util.concurrent.TimeUnit

import scala.collection.immutable
import akka.util.Timeout
import akka.japi.Util._
import com.typesafe.config.{ConfigFactory, Config}
import org.apache.kafka.common.serialization.{StringSerializer, StringDeserializer}

/** TODO
  * 1. Replace hard-coded String serDe for kafka with [K,V]
  * 2. Make everything configurable
  */
class KafkaSettings(cfg: Config) {

  def this() = this(ConfigFactory.empty)

  final val config = cfg.withFallback(ConfigFactory.load())

  private final val c = config.getConfig("continuum")

  final val CreationTimeout: Timeout = Timeout(8000, TimeUnit.MILLISECONDS)

  final val SourceRoutingPrefix: String = c.getString("routing.source.prefix")

  final val SinkRoutingPrefix: String = c.getString("routing.sink.prefix")

  /*kafka {

    log.dir = ""
    replica.high.watermark.checkpoint.interval.ms = 5000
    log.flush.interval.messages = 1
    replica.socket.timeout.ms = 1500
    controlled.shutdown.enable = true

    publisher {
    }
    consumer {
    }*/

  /* TODO productionize everything below. This is just initial PoC status. */

  final val KafkaHostname: String = c.getString("kafka.host.name")
  
  final val KafkaConnect: IndexedSeq[String] = immutableIndexedSeq(c.getStringList("kafka.connect"))

  final val KafkaSeedNodes = KafkaConnect.mkString(",")

  final val KafkaZookeeperConnect: IndexedSeq[String] = immutableIndexedSeq(c.getStringList("kafka.zookeeper.connect"))

  final val KafkaLogDir = c.getString("kafka.log.dir")

  lazy val brokerConfig: Map[String,String] =
    Map(//TODO all of these configs
      //"broker.id" -> "0",
      "host.name" -> KafkaHostname,
      "metadata.broker.list" -> KafkaSeedNodes,
      "advertised.host.name" -> KafkaSeedNodes,
      "advertised.port" -> "9092",
      "log.dir" -> KafkaLogDir,
      "log.dirs" -> KafkaLogDir,
      "zookeeper.connect" -> KafkaZookeeperConnect.mkString(","),
      "replica.high.watermark.checkpoint.interval.ms" -> "5000",
      "log.flush.interval.messages" -> "1",
      "replica.socket.timeout.ms" -> "1500",
      "controlled.shutdown.enable" -> "true")

  lazy val producerConfig: Map[String,String] =
    Map(
      "bootstrap.servers" -> KafkaConnect.mkString(","), //"client.id" -> "",
      "key.serializer" -> classOf[StringSerializer].getName,
      "value.serializer" -> classOf[StringSerializer].getName
    )

  lazy val consumerConfig: immutable.Map[String,String] =
    Map(
      "bootstrap.servers" -> KafkaConnect.mkString(","),
      "group.id" -> "consumer.group",
      "auto.offset.reset" -> "latest",//latest,earliest,none
      "enable.auto.commit" -> "true",
      "key.deserializer" -> classOf[StringDeserializer].getName,
      "value.deserializer" -> classOf[StringDeserializer].getName)

}
