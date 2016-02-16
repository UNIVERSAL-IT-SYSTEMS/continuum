package examples

import scala.concurrent.duration._
import akka.actor._
import akka.event.{LoggingAdapter, Logging}
import com.tuplejump.continuum.{Assertions, Kafka}
import com.tuplejump.continuum.Events._
import com.tuplejump.continuum.Commands._

object Bootstrap extends App {

  new Thread() {
    override def run(): Unit =
      IngestionApp.main(args)
  }.start()

  new Thread() {
    override def run(): Unit =
      AnalyticsApp.main(args)
  }.start()
}

object IngestionApp {

  def main(args: Array[String]): Unit = {

    val eventLog: EventLog = EventLogger.create()

    val ingestor = new BidServer(eventLog, Simulation(2.seconds, 1000, 50, 10))

  }

  final case class Simulation(interval: FiniteDuration, bursts: Int, totalSize: Int, groupSize: Int)
  final case class BidRequest(json: String) extends SourceEvent// just a string for simplicity ATM

}

object EventLogger {

  val topic = "topic"

  val system = Node.create(port = 2560)

  /** 1. Create the extension and the node's actor system */
  val kafka = Kafka(system)

  val adapter = Logging(kafka.system, "EventLogger")

  def create(): EventLog = {
    // Creates topic in Kafka for simulation. In prod the topic would already
    // exists and be configured with the appropriate security, partitioning, etc.
    NodeLifecycleKafka.simulation(kafka.settings, Set(topic))

    /** 2. Create your custom handler of any type which receives and logs data. */
    new EventLogKafka(kafka, adapter)
  }
}

object AnalyticsApp extends Assertions {

  def main(args: Array[String]): Unit = {
    val system = Node.create(2560)

    /** 3. Create any custom receiver(s) that are an [[akka.actor.Actor]]. */
    val processor = StreamProcessor(system)

    import examples.EventLogger.{kafka, topic}

    /** 4. Create a kafka sink, which subscribes to the provided topics.
      * On receive on new events this will pass data to one or more processors
      * for custom work and compute logic. */
    kafka.sink(List(topic), processor)
  }
}

/** Black box. */
trait EventLog {
  def log(data: SourceEvent): Unit
}

class EventLogKafka(kafka: Kafka, adapter: LoggingAdapter) extends EventLog {
  import IngestionApp.BidRequest

  val source = kafka.source(kafka.settings.producerConfig)

  adapter.info("{} started with source '{}'", this, source)

  def log(event: SourceEvent): Unit = event match {
    case BidRequest(data) =>
      source ! Log(data, "bid_requests")//TODO types vs comma-separated string
    case other =>
      adapter.error("Unroutable: received unsupported type {}", other)
  }
}

/** Just demonstrates the idea of another pathway for data. */
class EventLogSomeDataStore(source: AnyRef) extends EventLog {

  def log(event: SourceEvent): Unit = {
    // source Store(event.data...)
  }
}

/** Simulates ingesting a sequence of BidRequest data every `interval` seconds.
  * Hands data to the eventLog in simulated bursty chunks.
  *
  * @param eventLog the event log to use
  * @param simulation the settings for the simulation
  */
class BidServer(eventLog: EventLog, simulation: IngestionApp.Simulation) {
  import System.{nanoTime => localTimestamp}
  import IngestionApp.BidRequest

  /** Creates the simulation data. */
  private def windows: Iterator[IndexedSeq[BidRequest]] = {
    val func = (n: Int) => BidRequest(s"message-$n-" + localTimestamp)
    (0 to simulation.totalSize)
      .map(func)
      .sliding(simulation.groupSize)
  }

  for {
    requests <- windows
    _        = Thread.sleep(simulation.interval.toMillis)
    event    <- requests
  } eventLog log event
}

object StreamProcessor {
  def apply(system: ActorSystem): ActorRef =
    system.actorOf(Props[StreamProcessor])
}

/** Asynchronously receives data from the Kafka stream for provided topic(s).
  * This would have your custom processing off the stream: analytics, machine learning, etc.
  */
class StreamProcessor extends Actor with ActorLogging {
  def receive: Actor.Receive = {
    case data: AnyRef => process(data)
  }

  private def process(data: AnyRef): Unit = {
    log.info("Received {}", data)
  }
}