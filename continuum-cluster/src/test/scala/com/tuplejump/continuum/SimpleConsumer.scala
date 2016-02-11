package com.tuplejump.continuum

import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import kafka.serializer.StringDecoder
import kafka.consumer.{ Consumer, ConsumerConfig }

import scala.util.Try

/**
  * Very simple consumer of a single Kafka topic.
  * TODO update to new consumer.
  */
class SimpleConsumer(zookeeper: String,
                     topic: String,
                     groupId: String,
                     partitions: Int,
                     numThreads: Int,
                     count: AtomicInteger) {

  val connector = Consumer.create(createConsumerConfig)

  val streams = connector
    .createMessageStreams(Map(topic -> partitions), new StringDecoder(), new StringDecoder())
    .get(topic)

  val executor = Executors.newFixedThreadPool(numThreads)

  for (stream <- streams) {
    executor.submit(new Runnable() {
      def run() {
        for (s <- stream) {
          while (s.iterator.hasNext) {
            count.getAndIncrement
          }
        }
      }
    })
  }

  private def createConsumerConfig: ConsumerConfig = {
    val props = new Properties()
    props.put("consumer.timeout.ms", "2000")
    //props.put("zookeeper.connect", zookeeper)
    props.put("group.id", groupId)
    //props.put("zookeeper.session.timeout.ms", "400")
    //props.put("zookeeper.sync.time.ms", "10")
    props.put("auto.commit.interval.ms", "1000")

    new ConsumerConfig(props)
  }

  def shutdown(): Unit = Try {
    connector.shutdown()
    executor.shutdown()
  }
}
