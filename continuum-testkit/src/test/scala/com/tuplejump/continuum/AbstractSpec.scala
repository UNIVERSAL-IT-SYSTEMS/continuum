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

import org.scalatest._
import org.scalatest.concurrent.Eventually

import scala.concurrent.duration._
import scala.concurrent.{Await, Awaitable}
import scala.util.control.NoStackTrace

/** Basic unit test abstraction. */
trait AbstractSpec extends WordSpecLike with TestHelper
  with Matchers with BeforeAndAfter with BeforeAndAfterAll with Eventually

trait FunctionalSpec extends Suite with FreeSpecLike
  with Matchers with BeforeAndAfter with BeforeAndAfterAll with Eventually

trait TestHelper {

  final val timeoutDuration = 10.seconds

  implicit class AwaitHelper[T](awaitable: Awaitable[T]) {
    def await: T = Await.result(awaitable, timeoutDuration)
  }

  val failure = new Exception("boom") with NoStackTrace

}