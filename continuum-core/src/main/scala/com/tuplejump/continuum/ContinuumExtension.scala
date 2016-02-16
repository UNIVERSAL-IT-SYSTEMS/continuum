package com.tuplejump.continuum

import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.Future
import akka.actor._
import akka.event.Logging

object ContinuumExtension extends ExtensionId[ContinuumExtension] with ExtensionIdProvider {

  override def get(system: ActorSystem): ContinuumExtension = super.get(system)

  override def lookup: ExtensionId[ContinuumExtension] = ContinuumExtension

  override def createExtension(system: ExtendedActorSystem): ContinuumExtension = new ContinuumExtension(system)

  override def apply(system: ActorSystem): ContinuumExtension =
    new ContinuumExtension(system.asInstanceOf[ExtendedActorSystem])
}

class ContinuumExtension(val system: ExtendedActorSystem) extends Extension { extension =>
  import system.dispatcher

  private implicit val sys: ActorSystem = system

  system.registerOnTermination(shutdown())

  protected val log = Logging(system, "Continuum")

  protected val isRunning = new AtomicBoolean(false)

  log.info("Starting node")
  isRunning.set(true)

  private def terminate(): Future[Terminated] =
    system.terminate()

  /** Shutdown is triggered automatically on [[akka.actor.ActorSystem.terminate()]]. */
  private def shutdown(): Unit = if (isRunning.get) {
    log.info("Node shutting down")
    // TODO graceful shutdown
  }
}


/** Reports on health of the node via configurable update strategy. */
private[continuum] class NodeHealth extends Actor with ActorLogging {
  import Model._

  private var status = NodeHealth()

  /* if on a task of configurable interval:
   task { context.system.eventStream.publish(status) } */

  def receive: Actor.Receive = {
    case e => /* else: sender() ! status */
  }
}
