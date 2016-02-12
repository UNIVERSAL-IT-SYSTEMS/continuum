package com.tuplejump.continuum

import scala.concurrent.duration._

trait TaskHandler {
  def interval: FiniteDuration
  def limit: Int
}
