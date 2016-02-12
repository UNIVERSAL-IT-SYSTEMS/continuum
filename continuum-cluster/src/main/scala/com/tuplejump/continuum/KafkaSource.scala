package com.tuplejump.continuum

import java.util.Properties

import akka.actor.{ActorLogging, Actor, Props}
import kafka.common.KafkaException

object KafkaSource extends JConverters {

  /** Simple String data publisher for today. */
  def props(config: Map[String,String]): Props =
    props(toProperties(config))

  /** Simple String data publisher for today. */
  def props(config: Properties): Props =
    Props(new KafkaSource(config))

}

/** Simple String data publisher for the moment.
  *
  * TODO pattern. Send to router for round robin or allow user to set any actors for distribution.
  * TODO supervision for error handling.
  * TODO [K,V : ClassTag] */
class KafkaSource(config: Properties) extends Actor with ActorLogging {
  import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

  /** Initial sample just using String.
    *
    * @see [[org.apache.kafka.clients.producer.KafkaProducer]]
    */
  private val producer =
    try new KafkaProducer[String, String](config)
    catch { case e: KafkaException => log.error(e, e.getMessage); throw e }

  /* TODO with a key and as [K,V] */
  private val toRecord = (e: SourceEvent) => e.partition match {
    case Some(k) => new ProducerRecord[String, String](e.to, k, e.data)
    case _       => new ProducerRecord[String, String](e.to, e.data)
  }

  override def postStop(): Unit = {
    log.info("Shutting down {}", producer)
    producer.close()
  }

  def receive: Actor.Receive = {
    case e: SourceEvent => publish(e)
  }

  /** Send the array of messages to the Kafka broker. Completely simple for today, do later. */
  def publish(e: SourceEvent): Unit = {
    val record = toRecord(e)
    log.debug("Attempting to send {}, {}", record, e.to)
    producer.send(record)
  }
}