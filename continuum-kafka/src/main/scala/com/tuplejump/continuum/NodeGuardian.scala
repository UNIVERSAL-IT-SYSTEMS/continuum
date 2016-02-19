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

import akka.actor._
import akka.util.Timeout

/** INTERNAL API.
  * TODO error handling strategy.*/
private[continuum] class NodeGuardian(settings: KafkaSettings) extends KafkaActor {
  import settings._
  import ClusterProtocol._,InternalProtocol._

  implicit val system: ActorSystem = context.system

  //implicit val timeout: Timeout = settings.CreationTimeout

  private lazy val defaultProducer: ActorRef = {
    val actor = context.system.actorOf(KafkaSource.props(producerConfig), "kafka-source")
    actor ! Initialize
    actor
  }

  def receive: Actor.Receive = {
    case GetDefaultSource =>
      sender() ! defaultProducer

    case CreateSource(config) =>
      sender() ! source(config)

    case CreateSink(topics, consumers, config) =>
      sender() ! sink(topics, consumers, config)

    case e: SourceEvent =>
      defaultProducer forward e

    case Unsubscribe(target) =>
      target ! PoisonPill
  }

  /** TODO only create new if `config` differs from first. */
  private def source(config: Map[String,String]): ActorRef =
    if (config.isEmpty) defaultProducer
    else context.system.actorOf(KafkaSource.props(config ++ settings.producerConfig))

  private def sink(topics: Set[String], consumers: Seq[ActorRef], config: Map[String,String]): ActorRef = {
    val props = KafkaSink.props(config ++ settings.consumerConfig, topics, consumers)
    context.system.actorOf(props)
  }
}

trait KafkaActor extends Actor with ActorLogging {

  override def postStop(): Unit = {
    log.info("Shutting down {}", self)
    super.postStop()
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.error(reason, s"Failure $message")
    super.preRestart(reason, message)
  }
}

