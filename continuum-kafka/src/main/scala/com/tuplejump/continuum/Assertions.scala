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

