package com.tuplejump.continuum

import java.util.concurrent.atomic.AtomicInteger

trait Emitter[A] {

  def emit: A
}

final class BidRequestEmitter extends Emitter[String] {

  private val atomic = new AtomicInteger(0)

  /** Stubbed out for expediency ATM. */
  val json = (id: Int) =>
    s"""
       |{
       |  "id":"$id"",
       |  "at":1,
       |  "cur":[
       |    "USD"
       |   ],
       |  "imp":[]
       |}
    """.stripMargin

  /** TODO as Events.BidRequest, have to do kafka type ser/des first. */
  def emit: String = json(atomic.getAndIncrement)

}
