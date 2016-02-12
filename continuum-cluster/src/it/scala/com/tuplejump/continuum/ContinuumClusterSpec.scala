package com.tuplejump.continuum

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import com.tuplejump.embedded.kafka.{Assertions, EmbeddedKafka}

object ContinuumClusterSpec extends Assertions {

  import scala.concurrent.duration._
  val batches = 10
  val size = 1000

  val topic = "simple-kafka-sample"
  val kafka = new EmbeddedKafka() // TODO in main() as expecting local or remote kafka cluster
  kafka.start()
  eventually(10000, 100)(assert(kafka.isRunning, "Kafka must be running."))
  kafka.createTopic(topic, 1, 1)

  implicit val system: ActorSystem = ActorSystem(topic)

  val cluster = ContinuumCluster(system)

  val settings = cluster.settings

  val consumers = TestProbe()//TODO add as actor

  def main(args: Array[String]): Unit = {
    import system.dispatcher

    val sink = cluster.sink(settings.consumerConfig, List(topic), consumers.ref)

    val source = cluster.source(settings.producerConfig)

    // publish data to kafka every 20 milliseconds
    val task = system.scheduler.schedule(Duration.Zero, 20.millis) {
      val data = for (n <- 0 until size) yield s"message-test-$n"
      cluster.log.info("Publishing {} messages.", data.size)
      //just one for now - adding millions and multi-jvm simulation on monday
      source ! SourceEvent(topic, data.mkString(","))//TODO serializer for type vs just string
    }

    val series = consumers.receiveWhile(max = 6000.millis, messages = size * batches) {
      case e: String =>
        cluster.log.info("Consumer received [{}]", e)
        e.split(",").toList
    }

    task.cancel()
  }

  Runtime.getRuntime.addShutdownHook(new Thread(s"Shutting down $topic") {
    override def run(): Unit = {
      system.terminate()
      eventually(5000, 5000)(assert(system.whenTerminated.isCompleted, "System should be shutdown."))
      kafka.shutdown()
    }
  })

}
