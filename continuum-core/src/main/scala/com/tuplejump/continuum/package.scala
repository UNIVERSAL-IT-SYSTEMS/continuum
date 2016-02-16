package com.tuplejump

import scala.concurrent.duration._
import scala.concurrent.{Await, Awaitable}
import akka.util.Timeout

package object continuum {

  implicit class AwaitHelper[T](awaitable: Awaitable[T])
                               (implicit val timeout: Timeout = Timeout(10.seconds)) {
    def await: T = Await.result(awaitable, timeout.duration)
  }
}
