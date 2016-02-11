package com.tuplejump.continuum

import scala.concurrent.Future
import akka.actor.{ActorRef, ActorSystem}
import com.rbmhtechnology.eventuate.crdt.{CRDTServiceOps, Counter, CounterService}

/** A simple capping service that increments only. Full in prog state is in a WIP branch.
  * This is just a placeholder.
  *
  * @param id the unique ID of this service
  *
  * @param log the event log
  */
class CappingService(val id: String, override val log: ActorRef)
                     (implicit val system: ActorSystem,
                      val integral: Integral[Int],
                      override val ops: CRDTServiceOps[Counter[Int], Int])
                      extends CounterService[Int](id, log) {

  import system.dispatcher

  /** Increment only op: adds `delta` to the counter identified by `id` and returns the updated counter value. */
  def increment(id: String, delta: Int): Future[Int] =
    value(id) flatMap {
      case v if v >= 0 && (delta > 0 || delta > v) =>
        update(id, delta)
      case v =>
        Future.successful(v)
    }

  start()

}

private final case class IncrementOp(delta: Any)

