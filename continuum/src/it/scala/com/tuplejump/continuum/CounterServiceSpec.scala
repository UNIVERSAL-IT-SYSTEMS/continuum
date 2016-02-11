package com.tuplejump.continuum

import akka.actor.ActorSystem
import akka.testkit.TestKit

class CounterServiceSpec extends TestKit(ActorSystem("counters"))
  with AbstractSpec with EventLogLifecycleLeveldb {

  import com.rbmhtechnology.eventuate.crdt._

  val id = "test"

  "A CounterService" must {
    "return the default value of a Counter" in {
      val service = new CounterService[Int](id, eventLog)
      service.value(id).await should be(0)
    }
    "increment a Counter" in {
      val service = new CounterService[Int](id, eventLog)
      service.update(id, 3).await should be(3)
      service.update(id, 2).await should be(5)
      service.value(id).await should be(5)
    }
    "decrement a Counter" in {
      val service = new CounterService[Int]("a", eventLog)
      service.update("a", -3).await should be(-3)
      service.update("a", -2).await should be(-5)
      service.value("a").await should be(-5)
    }
  }
}