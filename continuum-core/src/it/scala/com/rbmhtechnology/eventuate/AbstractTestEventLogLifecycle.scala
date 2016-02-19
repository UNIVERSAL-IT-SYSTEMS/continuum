package com.rbmhtechnology.eventuate

import com.rbmhtechnology.eventuate.log.{EventLog => EventuateEventLog}

trait AbstractTestEventLogLifecycle {

  val ErrorSequenceNr = -1L
  val IgnoreDeletedSequenceNr = -2L

  trait TestEventLog extends EventuateEventLog {

    def error: Exception

    override def currentSystemTime: Long = 0L

    override private[eventuate] def adjustFromSequenceNr(seqNr: Long) = seqNr match {
      case ErrorSequenceNr => seqNr
      case IgnoreDeletedSequenceNr => 0
      case s => super.adjustFromSequenceNr(s)
    }
  }
}