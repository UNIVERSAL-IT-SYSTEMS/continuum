package com.tuplejump.continuum

import java.util.concurrent.atomic.AtomicBoolean

import akka.util.Timeout

import scala.concurrent.Future
import akka.actor._
import akka.event.Logging

object ContinuumCluster extends ExtensionId[ContinuumCluster] with ExtensionIdProvider {

  override def get(system: ActorSystem): ContinuumCluster = super.get(system)

  override def lookup: ExtensionId[ContinuumCluster] = ContinuumCluster

  override def createExtension(system: ExtendedActorSystem): ContinuumCluster = new ContinuumCluster(system)

  override def apply(system: ActorSystem): ContinuumCluster =
    new ContinuumCluster(system.asInstanceOf[ExtendedActorSystem])
}

class ContinuumCluster(val system: ExtendedActorSystem) extends Extension { extension =>
  import akka.pattern.ask
  import system.dispatcher

  private implicit val sys: ActorSystem = system

  system.registerOnTermination(shutdown())

  private[continuum] val log = Logging(system, "ContinuumCluster")

  protected val _isRunning = new AtomicBoolean(false)

  protected val _isTerminated = new AtomicBoolean(false)

  private val kafka: Kafka = Kafka(system)

  val settings: KafkaSettings = kafka.settings

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
  def source(config: Map[String,String]): Future[ActorRef] =
    (guardian ? ClusterEvents.CreateSource(config)).mapTo[ActorRef]

  def sink(config: Map[String,String], topics: List[String], consumers: ActorRef*): Future[ActorRef] =
    (guardian ? ClusterEvents.CreateSink(config, topics, consumers.toSeq)).mapTo[ActorRef]

  private def terminate(): Future[Terminated] =
    system.terminate()

  /** Shutdown is triggered automatically on [[akka.actor.ActorSystem.terminate()]]. */
  private def shutdown(): Unit = if (!isTerminated) {
    if (_isTerminated.compareAndSet(false, true)) {
      log.info("Node shutting down")
      // TODO graceful shutdown
    }
  }
}

object ClusterEvents {

  /** TO DO */
  final case class NodeHealth() extends ContinuumEvent

  /** INTERNAL API. */
  private[continuum] final case class CreateSource(config: Map[String,String]) extends InternalContinuumEvent

  /** INTERNAL API. */
  private[continuum] final case class CreateSink(config: Map[String,String],
                                                 topics: List[String],
                                                 consumers: Seq[ActorRef]) extends InternalContinuumEvent
}