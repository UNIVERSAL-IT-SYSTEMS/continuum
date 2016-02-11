package com.tuplejump.continuum

import org.joda.time.{DateTimeZone, DateTime}

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