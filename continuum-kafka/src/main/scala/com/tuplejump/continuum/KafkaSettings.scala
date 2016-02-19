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

import scala.util.Try
import scala.concurrent.duration._
import scala.collection.immutable
import akka.util.Timeout
import com.typesafe.config.{ConfigFactory, Config}
import org.apache.kafka.common.serialization._

/** TODO everything. remove Try and handle cleanly. */
class KafkaSettings(cfg: Config) extends Logging {

  def this() = this(ConfigFactory.empty)

  final val conf = cfg.withFallback(ConfigFactory.load())

  final implicit val timeout: Timeout =
    Timeout(getMillisDuration(conf, "continuum.creation.timeout"))

  final val KafkaPort: Int = Try(conf.getInt("continuum.kafka.port")).toOption getOrElse 9092

  final val KafkaHostName: String =
    Try(conf.getString("continuum.kafka.host.name")).toOption getOrElse "127.0.0.1"

  final val KafkaConnect: String =
    Try(conf.getString("continuum.kafka.connect")).toOption getOrElse KafkaHostName + ":" + KafkaPort

  final val KafkaZookeeperConnect: String =
    Try(conf.getString("continuum.kafka.zookeeper.connect")) getOrElse KafkaHostName + ":" + 2181

  logger.info(s"""
       |kafka.port[$KafkaPort]
       |kafka.host.name[$KafkaHostName]
       |kafka.connect[$KafkaConnect]
       |kafka.zookeeper.connect[$KafkaZookeeperConnect]
     """.stripMargin)

  lazy val producerConfig: Map[String,String] =
    Map(
      "bootstrap.servers" -> KafkaConnect,
      "key.serializer" -> classOf[StringSerializer].getName,
      "value.serializer" -> classOf[ByteArraySerializer].getName)

  lazy val consumerConfig: immutable.Map[String,String] =
    Map(
      "bootstrap.servers" -> KafkaConnect,
      "group.id" -> "consumer.group",
      "auto.offset.reset" -> "latest",//latest,earliest,none
      "enable.auto.commit" -> "true",
      "key.deserializer" -> classOf[StringDeserializer].getName,
      "value.deserializer" -> classOf[ByteArrayDeserializer].getName)

  private def getMillisDuration(c: Config,path: String): FiniteDuration = getDuration(c, path, MILLISECONDS)

  private def getNanosDuration(c: Config, path: String): FiniteDuration = getDuration(c, path, NANOSECONDS)

  private def getDuration(c: Config, path: String, unit: TimeUnit): FiniteDuration =
    Duration(c.getDuration(path, unit), unit)
}
