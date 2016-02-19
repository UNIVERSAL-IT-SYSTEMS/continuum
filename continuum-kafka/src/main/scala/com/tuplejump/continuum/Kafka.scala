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

import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.Await
import akka.actor._
import akka.util.Timeout

/**
  * {{{
  *   val system = ActorSystem()
  *   val kafka = Kafka(system)
  * }}}
  */
object Kafka extends ExtensionId[Kafka] with ExtensionIdProvider {

  override def get(system: ActorSystem): Kafka = super.get(system)

  override def lookup: ExtensionId[Kafka] = Kafka

  override def createExtension(system: ExtendedActorSystem): Kafka = new Kafka(system)

  override def apply(system: ActorSystem): Kafka =
    new Kafka(system.asInstanceOf[ExtendedActorSystem])

}

final class Kafka(val system: ExtendedActorSystem) extends Extension with StringConverter { extension =>

  import akka.pattern.ask
  import ClusterProtocol._,InternalProtocol._

  implicit val sys: ActorSystem = system

  system.registerOnTermination(shutdown())

  protected val log = akka.event.Logging(system, "Kafka")

  protected val _isRunning = new AtomicBoolean(false)

  protected val _isTerminated = new AtomicBoolean(false)

  val settings = new KafkaSettings(system.settings.config)
  import settings._

  private [continuum] val guardian: ActorRef =
    system.actorOf(Props(new NodeGuardian(settings)))

  log.info("Starting node")
  _isRunning.set(true)

  def isRunning: Boolean = _isRunning.get


  /** Uses the default producer from provided configuration.
    * If the producer has already been created, that instance is used,
    * otherwise it creates the default and uses that.
    *
    * To use the default configured producer:
    * {{{
    *   kafka.log(data, to)
    * }}}
    *
    * TODO partition key as typed vs string
    */
  def log(data: String, topic: String, partition: Option[String] = None): Unit =
    log(SourceEvent(classOf[String], to(data), topic, partition))

  def log(event: SourceEvent): Unit =
    guardian ! event

  /** Creates if not yet created, returns the default producer vs create a new one. */
  def source: ActorRef =
    request(GetDefaultSource)

  /** Configures and creates a new producer if `config` is not empty.
    * Returns the default producer with no additional config, other than
    * what is supplied in application load time, if `config` is empty.
    * Blocks during creation.
    *
    * To create a new producer with additional and or override configurations from
    * the defaults set at application load time:
    * {{{
    *   val additionalOrOverrideConfig = Map(k -> v)
    *   val source = kafka.source(additionalOrOverrideConfig)
    * }}}
    */
  def source(config: Map[String,String]): ActorRef =
    request(CreateSource(config))

  /** Configures and creates a new consumer. Blocks during creation. */
  def sink(topics: Set[String], consumers: ActorRef*)
          (implicit config: Map[String,String] = Map.empty): ActorRef =
    request(CreateSink(topics, consumers.toSeq, config))

  /** Shuts down the consumer. */
  def unsubscribe(target: ActorRef): Unit =
    guardian ! Unsubscribe(target)

  /** INTERNAL API. */
  private def request(resource: ResourceCommand): ActorRef =
    Await.result((guardian ? resource).mapTo[ActorRef], timeout.duration)

  /** Shutdown is triggered automatically on [[akka.actor.ActorSystem.terminate()]]. */
  private def shutdown(): Unit = {
    if (_isRunning.compareAndSet(true, false)) {
      log.info("Node shutting down")
      guardian ! PoisonPill
    }
  }
}

