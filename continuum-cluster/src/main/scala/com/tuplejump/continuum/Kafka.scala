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

  //  import settings._
  //  import statements._

  //private var _session: Session = _

 /* Try {
   //connect
   //create topics/configs needed
   //do any setup needed
   //_session = ...
  } match {
    case Success(_) => logging.info("Kafka extension initialized")
    case Failure(e) =>
      logging.error(e, "Kafka extension initialization failed")
      terminate()
      throw e
  }*/

  /* @annotation.tailrec
   private def connect(retries: Int = 0): Session = {
     val curAttempt = retries + 1
     val maxAttempts = settings.connectRetryMax + 1

     Try(connect to cluster..) match {
       case Failure(e: Throwable) if retries < settings.connectRetryMax => //NoHostAvailableException
         logging.error(e, s"Cannot connect to cluster (attempt $curAttempt/$maxAttempts)")
         Thread.sleep(settings.connectRetryDelay.toMillis)
         connect(retries + 1)
       case Failure(e) =>
         logging.error(e, s"Cannot connect to cluster (attempt $curAttempt/$maxAttempts)")
         throw e
       case Success(conn) =>
         conn
     }
   }*/

  system.registerOnTermination {
    //session.close()
    //cluster.close()
  }
}
