package com.tuplejump.continuum

import akka.actor.{ActorSystem, ActorRef, ActorLogging, Actor}
import akka.util.Timeout

private[continuum] class NodeGuardian(settings: KafkaSettings) extends Actor with ActorLogging {

  implicit val system: ActorSystem = context.system

  implicit val timeout: Timeout = settings.CreationTimeout

  def receive: Actor.Receive = {
    case ClusterEvents.CreateSource(config) =>
      sender() ! source(config)

    case ClusterEvents.CreateSink(config, topics, consumers) =>
      sender() ! sink(config, topics, consumers)
  }

  private def source(config: Map[String,String]): ActorRef =
    context.system.actorOf(KafkaSource.props(config))

  private def sink(config: Map[String,String], topics: List[String], consumers: Seq[ActorRef]): ActorRef =
    context.system.actorOf(KafkaSink.props(config, topics, consumers))

}

/** Reports on health of the node via configurable update strategy. */
private[continuum] class NodeHealth(settings: KafkaSettings) extends Actor with ActorLogging {

  private var status = ClusterEvents.NodeHealth()

  /* if on a task of configurable interval:
   task { context.system.eventStream.publish(status) } */

  def receive: Actor.Receive = {
    case e => /* else: sender() ! status */
   }
}