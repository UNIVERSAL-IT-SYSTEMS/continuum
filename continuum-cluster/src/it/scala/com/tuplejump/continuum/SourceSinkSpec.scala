package com.tuplejump.continuum

import scala.util.Random
import akka.actor.ActorSystem
import akka.testkit.TestProbe
import com.tuplejump.embedded.kafka.{Assertions, EmbeddedKafka}

class SourceSinkSpec extends AbstractSpec with Assertions {

  val random = new Random(0)

  val topics = List(random.nextInt.toString)

  val kafka = new EmbeddedKafka()

  implicit val system: ActorSystem = ActorSystem("test")

  val cluster = ContinuumCluster(system)

  val settings = cluster.settings

  //TODO LBR of actors
  val consumers = TestProbe()

  override def beforeAll(): Unit = {
    kafka.isRunning should be (false)
    kafka.start()
    eventually(10000, 100)(assert(kafka.isRunning, "Kafka must be running."))
    topics foreach (kafka.createTopic(_, 1, 1))
  }

  override def afterAll(): Unit = {
    system.terminate()
    eventually(5000, 5000)(assert(system.whenTerminated.isCompleted, "System should be shutdown."))
    kafka.shutdown()
  }

  "Initial" must {
    "publish messages to the embedded kafka instance" in {

      import system.dispatcher

      import scala.concurrent.duration._
      val batches = 10
      val size = 1000

      val sink = cluster.sink(settings.consumerConfig, topics, consumers.ref)

      val source = cluster.source(settings.producerConfig)

      val task = system.scheduler.schedule(Duration.Zero, 1.seconds) {
        val data = for (n <- 0 until size) yield s"message-test-$n"
        cluster.log.info("Publishing {} messages.", data.size)
        //just one for now
        for (to <- topics.headOption) {
          source ! SourceEvent(to, data.mkString(","))//TODO serializer for type vs just string
        }
      }

      val series = consumers.receiveWhile(max = 6000.millis, messages = size * batches) {
        case e: String =>
          cluster.log.info("Consumer received [{}]", e)
          val received = e.split(",").toList
          received.size should be (size)
          received
      }

      series.forall(_.size == size) should be (true)

      task.cancel()
    }
  }
}
