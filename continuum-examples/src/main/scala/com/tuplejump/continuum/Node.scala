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

import scala.concurrent.duration._
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import examples.Example.{BidRequest, Simulation}
import examples.{BiddingServer, Example}

object Node extends LocalRemote with Assertions {

  def isRunningLocalZk: Boolean =
    isRunningLocal("zookeeper", "config/zookeeper.properties")

  def isRunningLocalKafka: Boolean =
    isRunningLocal("kafka", "config/server.properties")

  def create(name:String, port: Int): ActorSystem = {

    val config = ConfigFactory.parseString(
        s"akka.remote.netty.tcp.port=$port")
      .withFallback(ConfigFactory.load())

    val as = ActorSystem(name, config)

    sys.runtime.addShutdownHook(new Thread(s"Shutting down.") {
      override def run(): Unit = as.terminate()
    })
    as
  }

  /** Handles example simulation work that would normally be done by automated cluster components. */
  def run(simulation: Simulation, settings: KafkaSettings, system: ActorSystem, microservice: BiddingServer): Unit = {
    import System.{nanoTime => nanos}

    import system.dispatcher

    topicLifecycle(settings.KafkaZookeeperConnect, Example.topics)

    /* Creates the simulation data. Strings only (this week). */
    def burst: Iterator[IndexedSeq[BidRequest]] = {
      val func = (n: Int) => new BidRequest(s"request-$n-" + nanos)
      (0 to simulation.totalSize)
        .map(func)
        .sliding(simulation.burstSize)
    }

    /* Triggers generation of data to ingest. */
    val task = system.scheduler.schedule(2.seconds, simulation.interval){
      for (requests <- burst) microservice.receive(requests:_*)
    }
    system.registerOnTermination(task.cancel)
  }

  /** Creates topic in Kafka and removes during shutdown. In prod the topic would already
    * exists and be configured with the appropriate security, partitioning, etc.
    */
  def topicLifecycle(zookeeperConnect: String, topics: Set[String]): Unit = {
    import kafka.admin.AdminUtils
    import kafka.common.{TopicAlreadyMarkedForDeletionException, TopicExistsException}
    import kafka.utils.ZkUtils

    val zk = ZkUtils(zookeeperConnect, 6000, 6000, false)

    topics foreach { topic =>
      try {
        AdminUtils.createTopic(zk, topic, 1, 1)
        await(5000,300)(AdminUtils.topicExists(zk, topic))
      } catch { case e: TopicExistsException => /* ignore */ }
    }

    sys.runtime.addShutdownHook(new Thread(s"Cleanup ${getClass.getName}"){
      override def run(): Unit = {
        for (topic <- topics) {
          try AdminUtils.deleteTopic(zk, topic)
          catch { case e: TopicAlreadyMarkedForDeletionException => /* ignore */ }
        }
        zk.close()
      }
    })
  }
}
