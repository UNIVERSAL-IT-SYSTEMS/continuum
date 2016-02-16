package com.tuplejump.continuum

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor._
import akka.event.Logging
import akka.util.Timeout

import scala.concurrent.Await

object Kafka extends ExtensionId[Kafka] with ExtensionIdProvider {

  override def get(system: ActorSystem): Kafka = super.get(system)

  override def lookup: ExtensionId[Kafka] = Kafka

  override def createExtension(system: ExtendedActorSystem): Kafka = new Kafka(system)

  override def apply(system: ActorSystem): Kafka =
    new Kafka(system.asInstanceOf[ExtendedActorSystem])

}

class Kafka(val system: ExtendedActorSystem) extends Extension with Assertions { extension =>

  import akka.pattern.ask

  implicit val sys: ActorSystem = system

  system.registerOnTermination(shutdown())

  private[continuum] val log = Logging(system, "ContinuumKafka")

  protected val _isRunning = new AtomicBoolean(false)

  protected val _isTerminated = new AtomicBoolean(false)

  val settings = new KafkaSettings(system.settings.config)

  implicit val timeout: Timeout = settings.CreationTimeout

  private [continuum] val guardian: ActorRef =
    system.actorOf(Props(new NodeGuardian(settings)))

  log.info("Starting node")
  _isRunning.set(true)

  def isRunning: Boolean = _isRunning.get

  def isTerminated: Boolean = _isTerminated.get

  /* To Do: type these configs for compile time checks WRT versioning */

  /** You only need to create one instance unless you want > 1, each with differing configuration or
    * for example, different levels of security. It is defined for now by a function vs val.
    */
  def source(config: Map[String,String] = Map.empty): ActorRef =
    Await.result((guardian ? InternalClusterCommands.CreateSource(config)).mapTo[ActorRef], timeout.duration)

  def sink(topics: List[String], consumers: ActorRef*)(implicit config: Map[String,String] = Map.empty): ActorRef =
    Await.result((guardian ? InternalClusterCommands.CreateSink(topics, consumers.toSeq, config)).mapTo[ActorRef], timeout.duration)

  /** Shutdown is triggered automatically on [[akka.actor.ActorSystem.terminate()]]. */
  private def shutdown(): Unit = if (!isTerminated) {
    if (_isTerminated.compareAndSet(false, true)) {
      log.info("Node shutting down")
      // TODO graceful shutdown
    }
  }
}
