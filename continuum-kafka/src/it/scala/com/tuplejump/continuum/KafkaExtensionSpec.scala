package com.tuplejump.continuum

import scala.concurrent.duration._
import akka.testkit.TestProbe

class KafkaExtensionSpec extends AbstractKafkaSpec("test.kafkaextension") with StringConverter {
  import ClusterProtocol._
  import System.{nanoTime => nanos}
  import system.dispatcher

  private val batches = 100
  private val expected = 1000
  private val to = topic

  "A KafkaExtension" must {
    val kafka = Kafka(system)

    val pack = (n: Int) => SourceEvent(classOf[String], to(s"message-$n-$nanos"), to)
    val unpack = (bytes: Array[Byte]) => from(bytes)

    "publish messages to kafka with the default producer" in {
      val source = kafka.source
      val probe = TestProbe()
      val sink = kafka.sink(Set(topic), consumers = probe.ref)

      val task = system.scheduler.schedule(initialDelay, 300.millis) {
        logger.info(s"Publishing $expected messages to 'source'.")

        for (n <- 0 until expected) {
          val data = pack(n)
          source ! data
        }
      }

      val series = probe.receiveWhile(max = 10.seconds, messages = expected) {
        case SinkEvent(_, data, offset, from,_) =>
          from should be (topic)
          val received = unpack(data)
          received.contains("message-") should be (true)
          logger.info(s"Consumer received $data from $topic at $offset")
          received
      }
      series.size should === (expected)
      task.cancel()
      kafka unsubscribe sink
    }
    "publish messages to kafka with the default producer using 'log'" in {
      val probe = TestProbe()
      val sink = kafka.sink(Set(topic), probe.ref)

      val task = system.scheduler.schedule(initialDelay, 300.millis) {
        logger.info("Publishing messages via 'kafka.log'.")
        val data = pack(expected)
        kafka log data
      }

      val series = probe.receiveWhile(max = 10.seconds, messages = expected) {
        case SinkEvent(_, v, offset, from, partition) =>
          val data = unpack(v)
          logger.info(s"Consumer received $data from $topic at $offset")
          from should be (topic)

          data.contains("message-") should be (true)
          data
      }
      series.size should === (expected)
      task.cancel()
      kafka unsubscribe sink
    }
  }
}
