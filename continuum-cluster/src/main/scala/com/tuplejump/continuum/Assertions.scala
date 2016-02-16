package com.tuplejump.continuum


import java.util.concurrent.TimeoutException

import scala.annotation.tailrec
import scala.util.control.NonFatal

/** Simple helper assertions. */
trait Assertions {

  def await[T](timeout: Long, interval: Long)(func: => T): T = {
    def attemptT: Either[Throwable, T] =
      try Right(func) catch { case NonFatal(e) => Left(e) }

    val startTime = System.currentTimeMillis

    @tailrec
    def retry(attempt: Int): T = {
      attemptT match {
        case Right(result) => result
        case Left(e) =>
          val duration = System.currentTimeMillis - startTime

          if (duration < timeout) Thread.sleep(interval)
          else throw new TimeoutException(e.getMessage)

          retry(attempt + 1)
      }
    }

    retry(1)
  }
}
