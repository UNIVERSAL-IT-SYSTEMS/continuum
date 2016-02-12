package com.tuplejump.continuum

import akka.actor._
import akka.event.Logging

object Kafka extends ExtensionId[Kafka] with ExtensionIdProvider {

  override def get(system: ActorSystem): Kafka = super.get(system)

  override def lookup: ExtensionId[Kafka] = Kafka

  override def createExtension(system: ExtendedActorSystem): Kafka = new Kafka(system)

  override def apply(system: ActorSystem): Kafka =
    new Kafka(system.asInstanceOf[ExtendedActorSystem])

}

class Kafka(val system: ExtendedActorSystem) extends Extension { extension =>

  val settings = new KafkaSettings(system.settings.config)

  private val logging = Logging(system, "Kafka")

  system.registerOnTermination {

  }
}
