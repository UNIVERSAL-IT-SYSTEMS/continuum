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
class KafkaSink(config: Map[String,java.lang.Object], topics: List[String], consumers: Seq[ActorRef]) extends KafkaActor {

  import context.dispatcher
  import org.apache.kafka.clients.consumer.KafkaConsumer

  private val consumer = new KafkaConsumer[String, String](config.asJava)

  consumer.subscribe(topics.asJava)
  log.info("Subscribed to topics {}", topics)

  val task = context.system.scheduler.schedule(3.seconds, 1.seconds) {
    self ! ScheduledTasks.Tick
  }

  override def postStop(): Unit = {
    task.cancel()
    //TODO consumer.unsubscribe()
    consumer.close()
    super.postStop()
  }

  def receive: Actor.Receive = {
    case ScheduledTasks.Tick => poll()
  }

  /** Will be dependent upon a distribution pattern set by the user -...... TODO
    * for today, just broadcasting.
    */
  private def poll(): Unit = {
    val record = consumer.poll(1000)

    val data = record.iterator().asScala.map(_.value).toList

    log.info(s"Received offset[{}], partitions[{}], data[{}]", record.count(), record.partitions(), data)
    if (data.nonEmpty) {
      consumers foreach (_ ! data.mkString(","))
      //todo the type handling, send to consumers here or not.
    }
  }
}