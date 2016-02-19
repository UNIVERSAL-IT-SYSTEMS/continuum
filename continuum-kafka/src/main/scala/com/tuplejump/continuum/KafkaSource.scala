/*
 * Copyright 2016 Tuplejump
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tuplejump.continuum

import java.util.{Properties => JProperties}

import scala.util.control.NonFatal
import akka.actor.{Actor, Props}

object KafkaSource extends JPropertiesConverter {

  /** Simple String data publisher for today. */
  def props(config: Map[String,String]): Props =
    props(to(config))

  /** Simple String data publisher for today. */
  def props(config: JProperties): Props =
    Props(new KafkaSource(config))

}

/** Simple publisher for the moment.
  *
  * TODO pattern. Send to router for round robin or allow user to set any actors for distribution.
  * TODO supervision for error handling.
  */
class KafkaSource(config: JProperties) extends KafkaActor {
  import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
  import ClusterProtocol._,InternalProtocol._

  /** Initial sample just using String. */
  private var producer: Option[KafkaProducer[String,Array[Byte]]] = None

  override def postStop(): Unit = {
    log.info("Shutting down {}", producer)
    producer foreach (_.close)
    producer = None
    super.postStop()
  }

  def receive: Actor.Receive = {
    case Initialize => initialize
  }

  def initialized: Actor.Receive = {
    case e: SourceEvent => publish(e)
  }

  /** Send the array of messages to the Kafka broker. Completely simple for today, do later. */
  def publish(e: SourceEvent): Unit =
    producer.getOrElse(initialize).send(record(e))

  private val record = (e: SourceEvent) => e.partition match {
    case Some(k) => new ProducerRecord[String,Array[Byte]](e.to, k, e.data)
    case _       => new ProducerRecord[String,Array[Byte]](e.to, e.data)
  }

  /** Initial sample just using String for the moment. */
  private def initialize: KafkaProducer[String,Array[Byte]] =
    producer.getOrElse(try {
      val p = new KafkaProducer[String,Array[Byte]](config)
      context become initialized
      producer = Some(p)
      log.info("{} initialized with {}.", self.path.name, p)
      p
    } catch { case NonFatal(e) =>
      //TODO notify parent versus throw e
      log.error(e, e.getMessage); throw e
    })
}

