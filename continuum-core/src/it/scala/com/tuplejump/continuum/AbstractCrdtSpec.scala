package com.tuplejump.continuum

import akka.actor.ActorSystem
import akka.testkit.TestKit

import scala.concurrent.duration._
import akka.util.Timeout

class AbstractCrdtSpec(name: String) extends TestKit(ActorSystem(name))
  with AbstractSpec with EventLogLifecycleLeveldb {

  implicit val timeout: Timeout = Timeout(10.seconds)
}
