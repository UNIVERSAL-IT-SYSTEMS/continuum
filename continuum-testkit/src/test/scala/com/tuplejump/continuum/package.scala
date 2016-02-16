package com.tuplejump

import scala.concurrent.duration._
import scala.concurrent.{Await, Awaitable}

package object continuum {

  private val timeoutDuration = 10.seconds

  implicit class AwaitHelper[T](awaitable: Awaitable[T]) {
    def await: T = Await.result(awaitable, timeoutDuration)
  }
}
