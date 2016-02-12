package com.tuplejump.continuum

import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender

class MultiNodeCounterSpecMultiJvmNode1 extends MultiNodeCounterSpec
class MultiNodeCounterSpecMultiJvmNode2 extends MultiNodeCounterSpec
class MultiNodeCounterSpecMultiJvmNode3 extends MultiNodeCounterSpec

object MultiNodeCounterConfig extends MultiNodeConfig {
  val node1 = role("node1")
  val node2 = role("node2")
  val node3 = role("node3")

  testTransport(on = true)

  commonConfig(MultiNodeReplicationConfig.create(
    """
      |eventuate.log.write-batch-size = 200
      |eventuate.log.replication.remote-read-timeout = 2s
    """.stripMargin))
}

class MultiNodeCounterSpec extends MultiNodeSpec(MultiNodeCounterConfig)
  with MultiNodeWordSpec with ImplicitSender {

  muteDeadLetters(classOf[AnyRef])(system)

  def initialParticipants: Int = roles.size

  import MultiNodeCounterConfig._

  "A MultiNodeCounter" must {

    "wait for all nodes to enter a barrier" in {
      enterBarrier("startup")
    }
    "send to and receive from a remote node" in {
      runOn(node1) {
        enterBarrier("deployed")
      }

      runOn(node2) {
        enterBarrier("deployed")
      }

      runOn(node3) {
        enterBarrier("deployed")
      }

      enterBarrier("finished")
    }
  }
}
