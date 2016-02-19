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

package com.tuplejump.continuum

import org.joda.time.{DateTimeZone, DateTime}

object ClusterProtocol {

  sealed trait ContinuumEvent extends Serializable

  /** TODO partition key type vs string */
  final case class SourceEvent private(id: Identity,
                                       data: Array[Byte],
                                       to: String,
                                       partition: Option[String]) extends ContinuumEvent with Traceable

  object SourceEvent {

    def apply(eventType: Class[_], data: Array[Byte], to: String, partition: Option[String] = None): SourceEvent =
      SourceEvent(Identity(eventType), data, to, partition)
  }

  final case class SinkEvent(key: String, value: Array[Byte], offset: Long, topic: String, partition: Int) extends ContinuumEvent
  //TODO final case class SinkEvent(id: Identity, ...)

  /** Metadata: answers basic questions from the point of original ingestion which can be asked
    * at any point in its transit through systems.
    *
    * @param id         auto-generated nonce
    * @param classifier the event type
    * @param utcCreatedTimestamp    creation or ingestion timestamp
    */
  final case class Identity private(classifier: String,
                                    id: String,
                                    utcCreatedTimestamp: Long) extends Serializable

  private[continuum] object Identity {
    import java.security.SecureRandom
    import java.math.BigInteger

    private val random = new SecureRandom()

    private def next: String = new BigInteger(130, random).toString(32)

    /** TODO with optional partition key (system, not c*) */
    def apply(eventType: Class[_]): Identity = {
      val timestamp = new DateTime(DateTimeZone.UTC)
      val nonce = Identity.next
      Identity(eventType.getName, nonce, timestamp.getMillis)
    }

    /** Creates an ID from event source (eventType + utcTimestamp + nonce). */
    private def md5(value: String): String = {
      val id = java.security.MessageDigest.getInstance("MD5").digest(value.getBytes)
      id.map("%02X".format(_)).mkString.toLowerCase
    }
  }

  trait Traceable extends Serializable {
    def id: Identity
  }

  /** TO DO */
  final case class NodeHealth() extends Serializable

}

/** INTERNAL API. */
private[continuum] object ScheduledTasks {
  case object Tick
}
