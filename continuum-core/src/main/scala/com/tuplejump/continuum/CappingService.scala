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
