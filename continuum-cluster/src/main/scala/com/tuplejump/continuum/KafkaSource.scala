package com.tuplejump.continuum

import java.util.{Properties => JProperties}

import scala.util.control.NonFatal
import akka.actor.{ActorLogging, Actor, Props}

object KafkaSource extends JConverters {

  /** Simple String data publisher for today. */
  def props(config: Map[String,String]): Props =
    props(toProperties(config))

  /** Simple String data publisher for today. */
  def props(config: JProperties): Props =
    Props(new KafkaSource(config))

}

/** Simple String data publisher for the moment.
  *
  * TODO pattern. Send to router for round robin or allow user to set any actors for distribution.
  * TODO supervision for error handling.
  * TODO [K,V : ClassTag] */
class KafkaSource(config: JProperties) extends KafkaActor {
  import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
  import Commands._

  /** Initial sample just using String. */
  private val producer =
    try new KafkaProducer[String, String](config)
    catch { case NonFatal(e) => log.error(e, e.getMessage); throw e }

  log.info("{} created.", producer)

  override def postStop(): Unit = {
    log.info("Shutting down {}", producer)
    producer.close()
    super.postStop()
  }

  def receive: Actor.Receive = {
    case e: Commands.Log => publish(e)
  }

  /* TODO with a key and as [K,V] */
  private val toRecord = (e: Log) => e.partition match {
    case Some(k) => new ProducerRecord[String, String](e.to, k, e.data)
    case _       => new ProducerRecord[String, String](e.to, e.data)
  }

  /** Send the array of messages to the Kafka broker. Completely simple for today, do later. */
  def publish(e: Log): Unit = {
    val record = toRecord(e)
    log.debug("Attempting to publish [{}] to [{}].", record, e.to)
    producer.send(record)
  }
}