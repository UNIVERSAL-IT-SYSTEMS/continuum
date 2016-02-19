package com.tuplejump.continuum

import akka.testkit.TestProbe

import scala.concurrent.duration._

class KafkaExtensionSpec extends AbstractKafkaSpec("test.kafkaextension") with StringConverter {
  import ClusterProtocol._
  import System.{nanoTime => nanos}

  private val batches = 100
  private val expected = 1000
  private val to = topic

  "A KafkaExtension" must {
    import system.dispatcher

    val kafka = Kafka(system)

    val pack = (n: Int) => to(s"message-$n-$nanos")
    val unpack = (bytes: Array[Byte]) => from(bytes)

    "publish messages to kafka with the default producer" in {
      val source = kafka.source
      val probe = TestProbe()
      val sink = kafka.sink(Set(topic), consumers = probe.ref)

      val task = system.scheduler.schedule(initialDelay, 300.millis) {
        logger.info(s"Publishing $expected messages to 'source'.")

        for (n <- 0 until expected) {
          val data = pack(n)
          source ! SourceEvent(classOf[String], data, to)
        }
      }

      val series = probe.receiveWhile(max = 10.seconds, messages = expected) {
        case SinkEvent(_, data, _, from,_) =>
          from should be (topic)
          val received = unpack(data)
          received.contains("message-") should be (true)
          logger.info(s"Consumer received [$received]")
          received
      }
      series.size should be (expected)
      task.cancel()
    }
    /*"publish messages to kafka with the default producer using 'log'" in {
      val probe = TestProbe()
      val sink = kafka.sink(topics, consumers = probe.ref)

      val task = system.scheduler.schedule(initialDelay, 300.millis) {
        logger.info("Publishing messages via 'kafka.log'.")
        val data = pack(expected)
        topics.headOption.foreach(to => kafka.log(data, to))
      }

      val series = probe.receiveWhile(Duration.Undefined, messages = count) {
        case e: String =>
          logger.info(s"Consumer received [$e]")
          val received = unpack(e)
          received.size should be (expected)
          received
      }
      series.forall(_.size == expected) should be (true)
      task.cancel()
      kafka unsubscribe sink
    }*/
  }
}
