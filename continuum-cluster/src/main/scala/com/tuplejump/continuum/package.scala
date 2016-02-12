package com.tuplejump

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.{Duration, FiniteDuration}
import com.typesafe.config.Config

package object continuum {

  /** INTERNAL API. Taken from akka: */
  final implicit class ConfigOps(val config: Config) extends AnyVal {
    def getMillisDuration(path: String): FiniteDuration = getDuration(path, TimeUnit.MILLISECONDS)

    def getNanosDuration(path: String): FiniteDuration = getDuration(path, TimeUnit.NANOSECONDS)

    private def getDuration(path: String, unit: TimeUnit): FiniteDuration =
      Duration(config.getDuration(path, unit), unit)
  }

}