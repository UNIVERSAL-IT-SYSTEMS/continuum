package com.tuplejump.continuum

import akka.actor.ActorRef

object InternalClusterCommands {

  sealed trait InternalClusterCommand extends Serializable

  /** INTERNAL API. */
  final case class CreateSource(config: Map[String,String]) extends InternalClusterCommand

  object CreateSource {
    /** Creates a new instance with the default producer configuration. */
    def apply(): CreateSource = CreateSource(Map.empty[String,String])
  }

  /** INTERNAL API. */
  final case class CreateSink(topics: List[String], consumers: Seq[ActorRef], config: Map[String,String] = Map.empty) extends InternalClusterCommand

  object CreateSink {
    /** Creates a new instance with the default consumer configuration. */
    def apply(topics: List[String], consumers: Seq[ActorRef]): CreateSink =
      CreateSink(topics, consumers, Map.empty[String,String])
  }
}
