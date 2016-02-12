package com.tuplejump.continuum

import akka.actor.ActorSystem
import akka.event.Logging
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

private[continuum] class Node {

  val config =  ConfigFactory.parseString(
    """
      |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
      |akka.remote.netty.tcp.port=0
      |akka.remote.enabled-transports = ["akka.remote.netty.tcp"]
    """.stripMargin)

  implicit val system: ActorSystem = ActorSystem(getClass.getName, config)

  implicit val ec: ExecutionContext = system.dispatcher

  protected val log = Logging(system, system.name)

  def start(): Unit = {
    // on start, receives json or avro directly
    // passes to abstraction layer for distribution
  }

  def shutdown(): Unit = {
    system.terminate() 
  }

  Runtime.getRuntime.addShutdownHook(new Thread(s"Shutting down ${getClass.getSimpleName}") {
    override def run(): Unit = {
      shutdown()
    }
  })
}
