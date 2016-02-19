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

import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.duration._
import scala.collection.JavaConverters._
import akka.actor._
import akka.routing.SmallestMailboxPool

object KafkaSink {

  class SingleConsumer extends KafkaActor {
    def receive: Actor.Receive = {
      case e => log.info("Received {}", e) //TODO
    }
  }

  def router(id: String)(implicit system: ActorSystem): ActorRef =
    system.actorOf(SmallestMailboxPool(5).props(Props[SingleConsumer]))

  def props(config: Map[String,String], topics: Set[String], consumers: Seq[ActorRef])
           (implicit system: ActorSystem): Props =
    Props(new KafkaSink(config, topics, consumers))

}

/** TODO patterns of distribution! incredibly basic round 1.
  * Will be dependent upon a distribution pattern set by the user via config by sink.
  * TODO streams.
  * TODO supervision for error handling.
  * TODO types */
class KafkaSink(config: Map[String,java.lang.Object], topics: Set[String], consumers: Seq[ActorRef]) extends KafkaActor {
  import org.apache.kafka.clients.consumer.KafkaConsumer
  import ClusterProtocol._
  import context.dispatcher

  private val consumer = new KafkaConsumer[String, Array[Byte]](config.asJava)
  log.info("Created consumer [{}]", consumer)

  consumer.subscribe(topics.toList.asJava)
  log.info("Subscribed to topics [{}]", topics)

  val task = context.system.scheduler.schedule(3.seconds, 1.seconds)(self ! ScheduledTasks.Tick)

  override def postStop(): Unit = {
    task.cancel()
    //consumer.unsubscribe()
    consumer.close()
    super.postStop()
  }

  def receive: Actor.Receive = {
    case ScheduledTasks.Tick => poll()
  }

  private def poll(): Unit = {
    val record = consumer.poll(500) // TODO make configurable

    val received = record.iterator().asScala.toList
    if (received.nonEmpty) log.debug(
      s"Received offset[{}], partitions[{}], events[{}]", record.count, record.partitions, received.size
    )

    for (cr <- received) broadcast(cr.key, cr.value, cr.offset, cr.topic, cr.partition)
  }

  /** The only initial pattern supported. See the Roadmap, many more coming - configurable. */
  private def broadcast(key: String, value: Array[Byte], offset: Long, topic: String, partition: Int): Unit = {
    val event = SinkEvent(key, value, offset, topic, partition)
    consumers foreach (_ ! event)
  }
}

