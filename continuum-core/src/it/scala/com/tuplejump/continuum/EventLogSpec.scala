package com.tuplejump.continuum

import java.io.File

import scala.concurrent.duration._
import akka.actor._
import akka.pattern.ask
import akka.testkit._
import akka.util.Timeout
import com.rbmhtechnology.eventuate.DurableEvent
import com.rbmhtechnology.eventuate.log.BatchingLayer
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog
import com.typesafe.config.Config
import org.apache.commons.io.FileUtils
import org.scalatest._

import scala.util.Random

trait EventLogSpec extends Suite with BeforeAndAfterAll

trait EventLogCleanupLeveldb extends EventLogSpec {
  def config: Config

  def storageLocations: List[File] =
    List("eventuate.log.leveldb.dir", "eventuate.snapshot.filesystem.dir").map(s => new File(config.getString(s)))

  override def beforeAll(): Unit = {
    storageLocations.foreach(FileUtils.deleteDirectory)
    storageLocations.foreach(_.mkdirs())
  }

  override def afterAll(): Unit = {
    storageLocations.foreach(FileUtils.deleteDirectory)
  }
}

trait EventLogLifecycleLeveldb extends EventLogCleanupLeveldb with BeforeAndAfterEach {

  private var _logCtr: Int = 0
  private var _log: ActorRef = _

  override def beforeEach(): Unit = {
    super.beforeEach()

    _logCtr += 1
    _log = system.actorOf(logProps(logId))
  }

  override def afterEach(): Unit = {
    system.stop(eventLog)
    super.afterEach()
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

  def system: ActorSystem

  def config: Config =
    system.settings.config

  def batching: Boolean =
    true

  def eventLog: ActorRef =
    _log

  def logId: String =
    _logCtr.toString

  def logProps(logId: String): Props =
    RestarterActor.props(EventLogLifecycleLeveldb.TestEventLog.props(logId, batching))
}

object EventLogLifecycleLeveldb {

  object TestEventLog {
    def props(logId: String, batching: Boolean): Props = {
      val logProps = Props(new TestEventLog(logId)).withDispatcher("eventuate.log.dispatchers.write-dispatcher")
      if (batching) Props(new BatchingLayer(logProps)) else logProps
    }
  }

  class TestEventLog(id: String) extends LeveldbEventLog(id, "log-test")
    with TestHelper with TestEventLogLifecycle.TestEventLog {

    def error: Exception = failure

    override def unhandled(message: Any): Unit = message match {
      case "boom" =>
        throw error
      case "dir" =>
        sender() ! logDir
      case _ =>
        super.unhandled(message)
    }
  }
}

import com.rbmhtechnology.eventuate.AbstractTestEventLogLifecycle

object TestEventLogLifecycle extends AbstractTestEventLogLifecycle {
  import com.rbmhtechnology.eventuate.log.{BatchReadResult, EventLogClock}

  import scala.collection.immutable.Seq
  import scala.concurrent.Future

  trait MyTestEventLog extends TestEventLog {

    def error: Exception

    override def currentSystemTime: Long = 0L

    abstract override def read(fromSequenceNr: Long, toSequenceNr: Long, max: Int): Future[BatchReadResult] =
      if (fromSequenceNr == ErrorSequenceNr) Future.failed(error) else super.read(fromSequenceNr, toSequenceNr, max)

    abstract override def read(fromSequenceNr: Long, toSequenceNr: Long, max: Int, aggregateId: String): Future[BatchReadResult] =
      if (fromSequenceNr == ErrorSequenceNr) Future.failed(error) else super.read(fromSequenceNr, toSequenceNr, max, aggregateId)

    abstract override def replicationRead(fromSequenceNr: Long, toSequenceNr: Long, max: Int, filter: (DurableEvent) => Boolean): Future[BatchReadResult] =
      if (fromSequenceNr == ErrorSequenceNr) Future.failed(error) else super.replicationRead(fromSequenceNr, toSequenceNr, max, filter)

    abstract override def write(events: Seq[DurableEvent], partition: Long, clock: EventLogClock): Unit =
      if (events.map(_.payload).contains("boom")) throw error else super.write(events, partition, clock)

  }
}

class RestarterActor(props: Props, name: Option[String]) extends Actor with ActorLogging {

  import RestarterActor._

  val random = new Random(0)
  var child: ActorRef = newActor
  var requester: ActorRef = _

  override def receive: Actor.Receive = {
    case Restart =>
      log.info("Restarting")
      requester = sender()
      context.watch(child)
      context.stop(child)
    case Terminated(_) =>
      log.info("Terminated")
      child = newActor
      requester ! child
    case msg =>
      log.info("Forwarding {} to {}", msg, child)
      child forward msg
  }

  private def newActor: ActorRef =
    name.map(context.actorOf(props, _))
      .getOrElse(context.actorOf(props, random.nextInt.toString))

}

object RestarterActor extends TestHelper {
  case object Restart

  implicit val timeout: Timeout = Timeout(10.seconds)

  def restartActor(restarterRef: ActorRef): ActorRef =
    (restarterRef ? Restart).mapTo[ActorRef].await

  def props(props: Props, name: Option[String] = None): Props =
    Props(new RestarterActor(props, name))
}
