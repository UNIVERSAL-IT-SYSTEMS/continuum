package com.tuplejump.continuum

import scala.util.Random
import akka.actor.ActorSystem
import akka.testkit.TestProbe
import com.tuplejump.embedded.kafka.EmbeddedKafka

class SourceSinkSpec extends AbstractSpec with Assertions {

  val random = new Random(0)

  val topics = List(random.nextInt.toString)

  val embedded = new EmbeddedKafka()

  implicit val system: ActorSystem = ActorSystem("test")

  val kafka = Kafka(system)

  val settings = kafka.settings

  //TODO LBR of actors
  val consumers = TestProbe()

  override def beforeAll(): Unit = {
    embedded.isRunning should be (false)
    embedded.start()
    await(10000, 100)(assert(embedded.isRunning, "Kafka must be running."))
    topics foreach (embedded.createTopic(_, 1, 1))
  }

  override def afterAll(): Unit = {
    system.terminate()
    await(5000, 5000)(assert(system.whenTerminated.isCompleted, "System should be shutdown."))
    embedded.shutdown()
  }

  "Initial" must {
    "publish messages to the embedded kafka instance" in {

      import system.dispatcher

      import scala.concurrent.duration._
      val batches = 10
      val size = 1000

      val sink = kafka.sink(topics, consumers.ref)

      val source = kafka.source()

      val task = system.scheduler.schedule(Duration.Zero, 1.seconds) {
        val data = for (n <- 0 until size) yield s"message-test-$n"
        kafka.log.info("Publishing {} messages.", data.size)

        //TODO: just one for now, serializer for type vs just string
        for (to <- topics.headOption) {
          source ! Commands.Log(to, data.mkString(","))
        }
      }

      val series = consumers.receiveWhile(max = 6000.millis, messages = size * batches) {
        case e: String =>
          kafka.log.info("Consumer received [{}]", e)
          val received = e.split(",").toList
          received.size should be (size)
          received
      }

      series.forall(_.size == size) should be (true)

      task.cancel()
    }
  }
}
