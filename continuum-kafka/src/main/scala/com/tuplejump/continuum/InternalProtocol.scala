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

import akka.actor.ActorRef

/** INTERNAL API. */
private[continuum] object InternalProtocol {

  abstract class InternalCommand extends Serializable
  abstract class ResourceCommand extends InternalCommand

  case object Initialize extends InternalCommand
  final case class Subscribe(to: String, subscriber: ActorRef) extends ResourceCommand
  final case class Unsubscribe(subscriber: ActorRef) extends ResourceCommand

  case object GetDefaultSource extends ResourceCommand

  final case class CreateSource(config: Map[String,String]) extends ResourceCommand

  object CreateSource {
    /** Creates a new instance with the default producer configuration. */
    def apply(): CreateSource = CreateSource(Map.empty[String,String])
  }

  /** INTERNAL API. */
  final case class CreateSink(topics: Set[String],
                              consumers: Seq[ActorRef],
                              config: Map[String,String]) extends ResourceCommand

  object CreateSink {
    /** Creates a new instance with the default consumer configuration. */
    def apply(topics: Set[String], consumers: Seq[ActorRef]): CreateSink =
      CreateSink(topics, consumers, Map.empty[String,String])
  }
}
