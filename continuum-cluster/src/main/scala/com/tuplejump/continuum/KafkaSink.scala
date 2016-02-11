package com.tuplejump.continuum

import scala.concurrent.duration._
import scala.collection.JavaConverters._
import akka.actor._
import akka.routing.SmallestMailboxPool

object KafkaSink {

  class SingleConsumer extends Actor with ActorLogging {
    def receive: Actor.Receive = {
      case e => //TODO
        log.info("Received {}", e)
    }
  }

  def router(id: String)(implicit system: ActorSystem): ActorRef =
    system.actorOf(SmallestMailboxPool(5).props(Props[SingleConsumer]))

  def props(config: Map[String,String], topics: List[String], consumers: Seq[ActorRef])
           (implicit system: ActorSystem): Props = {

    Props(new KafkaSink(config, topics, consumers))
  }
}

/** TODO pattern. Send to router for round robin or allow user to set any actors for distribution.
  * TODO supervision for error handling.
  * TODO types */
class KafkaSink(config: Map[String,java.lang.Object], topics: List[String], consumers: Seq[ActorRef]) extends Actor with ActorLogging {

  import context.dispatcher
  import org.apache.kafka.clients.consumer.KafkaConsumer

  private val consumer = new KafkaConsumer[String, String](config.asJava)
  log.info("Subscribing to topics {}", topics)
  consumer.subscribe(topics.asJava)

  val task = context.system.scheduler.schedule(Duration.Zero, 1.seconds) {
    val record = consumer.poll(100)
    val data = record.iterator().asScala.map(_.value).toList
    if (data.nonEmpty) {
      log.info(s"Received offset[{}], partitions[{}], data[{}]", record.count(), record.partitions(), data)
      self ! data.mkString(",")//todo the type handling, send to consumers here or not.
    }
  }

  override def postStop(): Unit = {
    task.cancel()
    //TODO consumer.unsubscribe()
    consumer.close()
    super.postStop()
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.error(reason, s"Failure $message")
    super.preRestart(reason, message)
  }

  def receive: Actor.Receive = {
    case e: String => distribute(e)
  }

  private def distribute(e: String): Unit = {
    //entirely dependent upon a distribution pattern set by the user - all TODO
    //e.g. round robin, balancing pool, balancing group, smallest mailbox, hash...
    //for today, just broadcasting
    consumers foreach (_ ! e)
  }
}