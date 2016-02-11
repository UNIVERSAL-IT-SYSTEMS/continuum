package com.tuplejump.continuum

import akka.remote.testkit.MultiNodeSpecCallbacks
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest._

trait MultiNodeWordSpec extends MultiNodeSpecCallbacks with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()
}

object MultiNodeReplicationConfig {
  def create(customConfig: String = ""): Config = {
    val defaultConfig = ConfigFactory.parseString(
      s"""
         |akka.test.single-expect-default = 10s
         |akka.testconductor.barrier-timeout = 60s
         |akka.loglevel = "ERROR"
         |
         |eventuate.log.write-batch-size = 3
         |eventuate.log.replication.retry-delay = 1s
         |eventuate.log.replication.failure-detection-limit = 60s
         |
         |eventuate.snapshot.filesystem.dir = target/test-snapshot
         |
         |eventuate.log.leveldb.dir = target/test-log
         |eventuate.log.cassandra.default-port = 9142
         |eventuate.log.cassandra.index-update-limit = 3
         |eventuate.log.cassandra.table-prefix = mnt
       """.stripMargin)

    ConfigFactory.parseString(customConfig).withFallback(defaultConfig)
  }
}
