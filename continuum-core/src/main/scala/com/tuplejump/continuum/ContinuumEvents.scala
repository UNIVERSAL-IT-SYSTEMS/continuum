package com.tuplejump.continuum

private[continuum] abstract class ContinuumEvent extends Serializable

abstract class Traceable extends ContinuumEvent {
  def id: Identity
}

//TODO as bytes
final case class SourceEvent(to: String,
                             data: String,
                             partition: Option[String] = None) extends ContinuumEvent

final case class IngestionEvent[A](id: Identity, data: A) extends Traceable

/** INTERNAL API. */
abstract class InternalContinuumEvent extends ContinuumEvent
