package com.tuplejump.continuum

import org.joda.time.{DateTimeZone, DateTime}

object Events {
  import Model._

  sealed trait ContinuumEvent extends Serializable

  abstract class SourceEvent extends ContinuumEvent

  final case class IngestionEvent[A](id: Identity, data: A) extends Traceable

  /** INTERNAL API. */
  abstract class InternalContinuumEvent extends ContinuumEvent
}

private[continuum] object ScheduledTasks {
  case object Tick
}

object Commands {

  sealed trait ContinuumCommand extends Serializable

  //TODO data as bytes vs String & serDe
  final case class Log(data: String, to: String, partition: Option[String] = None) extends ContinuumCommand

  /**
    * @param data the data to store - just strings for today, changing this week
    * @param namespace a database, keyspace, etc. to use
    * @param table the table name to use
    */
  final case class Store(data: String, namespace: String, table: String) extends ContinuumCommand
}

object Model {

  /** Answers some basic questions from the point of original ingestion which can be asked
    * at any point in its transit through systems.
    *
    * @param id         auto-generated nonce
    * @param classifier the event type
    * @param created    creation or ingestion timestamp, in UTC
    */
  private[continuum] final case class Identity private(classifier: String, id: String, created: Long) extends Serializable

  private[continuum] object Identity {
    import java.security.SecureRandom
    import java.math.BigInteger

    private val random = new SecureRandom()

    private def next: String = new BigInteger(130, random).toString(32)

    /** TODO with optional partition key (system, not c*) */
    def apply(eventType: String): Identity = {
      val timestamp = new DateTime(DateTimeZone.UTC)
      val nonce = Identity.next
      Identity(eventType, nonce, timestamp.getMillis)
    }

    /** Creates an ID from event source (eventType + utcTimestamp + nonce). */
    private def md5(value: String): String = {
      val id = java.security.MessageDigest.getInstance("MD5").digest(value.getBytes)
      id.map("%02X".format(_)).mkString.toLowerCase
    }
  }

  abstract class Traceable extends Serializable {
    def id: Identity
  }

  /** TO DO */
  final case class NodeHealth() extends Serializable

}
