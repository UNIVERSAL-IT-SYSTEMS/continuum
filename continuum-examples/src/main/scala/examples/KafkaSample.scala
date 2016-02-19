/*
 * Copyright 2016 Tuplejump
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package examples

import scala.concurrent.duration._
import akka.actor._
import akka.event.Logging
import com.tuplejump.continuum._
import com.tuplejump.continuum.ClusterProtocol._

/** Simulates two nodes on different servers (not yet on multi-jvm simulation: Roadmap).
  * The runnable is [[KafkaExampleSimple]] which starts the others in different threads.
  *
  * Publishes to Kafka at:
  *   interval = 300.millis, burstSize = 1000, totalSize = 100000, Example.topics
  *
  * 1. Prerequisites: You must either have a local Zookeeper and Kafka server
  * running, or configure the necessary connection configurations to connect to
  * your remote cluster. If no brokers are available, java.net.ConnectException
  * is thrown from ZK/Kafka.
  *
  * 2. Configuration
  * For remote cluster: (typically these are read from the deploy environment,
  * variables set by chef, for instance, on deploy). The config file is merely
  * for fallbacks, defaults, test deploys etc.
  *
  * initial seed:
  * continuum.kafka.host.name = "127.0.0.1"
  *
  * Comma-separated list of kafka 'host:port' entries
  * continuum.kafka.connect = ["127.0.0.1:9092"]
  *
  * Comma-separated list of zookeeper 'host:port' entries
  * continuum.kafka.zookeeper.connect = ["127.0.0.1:2181"]
  *
  * The log dir to use, for example zk data dir
  * log.dir = ""
  *
  * 3. Run: from the command line:
  * {{{
  *   sbt examples/run
  * }}}
  *
  * Select examples.KafkaExampleSimple
  *
  * Or, simply run in an IDE.
  */
object KafkaExampleSimple extends App {

  new Thread() {
    override def run(): Unit =
      IngestionApp.main(Array("2560") ++ args)
  }.start()

  new Thread() {
    override def run(): Unit =
      AnalyticsApp.main(Array("2561") ++ args)
  }.start()
}

private object IngestionApp extends Logging {

  def main(args: Array[String]): Unit = {
    val system = Node.create("MicroserviceA", args(0).toInt)

    /** 1. Create the instance. */
    val kafka = Kafka(system)

    /** 2. Create your custom handler of any type which receives and logs data.
         Something like this could accept >1 destinations for data flows. */
    val eventLog = new EventLogger(kafka)

    /* Your microservice running on a node. */
    val microservice = new BiddingServer(eventLog)
    logger.info("Ingestion node started")

    val simulation = Example.Simulation(300.millis, 1000, 100000, Example.topics)
    Node.run(simulation, kafka.settings, system, microservice)
  }
}

private object AnalyticsApp {

  def main(args: Array[String]): Unit = {
    val system = Node.create("AnalyticsApp", args(0).toInt)
    val adapter = Logging(system, system.name)

    /** 1. Create the instance. */
    val kafka = Kafka(system)

    /** 2. Create any custom receiver(s) that are an [[akka.actor.Actor]]. */
    val processor = Subscriber("analytics", system)

    /** 3. Create a kafka sink, which subscribes to the provided topics.
      * On receive of new events this will pass data to one or more processors
      * for custom work and compute logic. In this simple example we pass in one. */
    kafka.sink(Example.topics, processor)
    adapter.info("AnalyticsApp node started")
  }
}

private final class EventLogger(kafka: Kafka) extends EventLogKafka(kafka) {
  import Example.BidRequest

  override def log(event: AnyRef): Unit =
    event match {
      case e: BidRequest =>
        val data = SourceEvent(BidRequest.etype, e.json.getBytes, BidRequest.to)
        kafka log data
      case other =>
        super.log(other)
    }
}

/** Simulates ingesting a sequence of BidRequest data every `interval` seconds.
  * Hands data to the eventLog in simulated bursty chunks.
  *
  * Executes:
  * {{{
  *   eventLog log event
  * }}}
  *
  * @param eventLog the event log to use
  */
class BiddingServer(eventLog: EventLogger) {
  import Example.BidRequest

  /** Sample showing sending to event log as data arrives vs batching. */
  def receive(requests: BidRequest*): Unit =
    for (event <- requests) eventLog log event

}

object Subscriber {
  import System.{nanoTime => nanos}

  def apply(role: String, system: ActorSystem): ActorRef =
    system.actorOf(Props[Subscriber], s"$role-subscriber-$nanos")
}

/** Asynchronously receives data from the Kafka stream for provided topic(s).
  * This would have your custom processing off the stream: analytics, machine learning, etc.
  */
class Subscriber extends Actor with ActorLogging with StringConverter {
  def receive: Actor.Receive = {
    case SinkEvent(_, data, offset, topic,_) => process(data, offset, topic)
  }

  /** Proceed with your custom processing streams. */
  private def process(bytes: Array[Byte], offset: Long, topic: String): Unit = {
    log.info("Received {} from {} at {}", from(bytes), topic, offset)
  }
}

object Example {

  val topics = Set(BidRequest.to)

  final case class Simulation(interval: FiniteDuration, burstSize: Int, totalSize: Int, topics: Set[String])
  final case class BidRequest(json: String) extends Serializable

  object BidRequest extends StringConverter {
    val etype = classOf[BidRequest]
    val to = etype.getSimpleName.toLowerCase
  }

  final case class Store(data: String, namespace: String, table: String)
}

