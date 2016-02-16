package com.tuplejump.continuum

import akka.actor.{ActorSystem, ActorRef, ActorLogging, Actor}
import akka.util.Timeout

private[continuum] class NodeGuardian(settings: KafkaSettings) extends Actor with ActorLogging {

  /* TODO error handling strategy. */

  implicit val system: ActorSystem = context.system

  implicit val timeout: Timeout = settings.CreationTimeout

  def receive: Actor.Receive = {
    case InternalClusterCommands.CreateSource(config) =>
      sender() ! source(config)

    case InternalClusterCommands.CreateSink(topics, consumers, config) =>
      sender() ! sink(topics, consumers, config)
  }

  private def source(config: Map[String,String]): ActorRef =
    context.system.actorOf(KafkaSource.props(config ++ settings.producerConfig))

  private def sink(topics: List[String], consumers: Seq[ActorRef], config: Map[String,String]): ActorRef =
    context.system.actorOf(KafkaSink.props(config ++ settings.consumerConfig, topics, consumers))

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