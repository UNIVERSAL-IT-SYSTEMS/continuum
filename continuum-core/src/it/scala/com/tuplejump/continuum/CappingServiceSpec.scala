package com.tuplejump.continuum

class CappingServiceSpec extends AbstractCrdtSpec("capping") {

  val id = "test"

  "A CappingService" must {
    "increment a Counter" in {
      val service = new CappingService(id, eventLog)
      service.increment(id, 0).await should be(0)
      service.increment(id, 3).await should be(3)
      service.increment(id, 2).await should be(5)
      service.value(id).await should be(5)
    }
    "not increment a Counter if the new value is not greater than the existing value" in {
      val service = new CappingService(id, eventLog)
      service.increment(id, 3).await should be(3)
      service.increment(id, -2).await should be(3)
      service.value(id).await should be(3)
    }
    "have the identical count between 2 instances in the same location" in {
      val locationAService1 = new CappingService(id, eventLog)
      locationAService1.increment(id, 3).await should be(3)
      locationAService1.increment(id, -2).await should be(3)
      locationAService1.value(id).await should be(3)

      val locationAService2 = new CappingService(id, eventLog)
      locationAService2.value(id).await should be(locationAService1.value(id).await)
    }
    "share the same value over 2 service-id instances for the same id" in {
      val instance1 = new CappingService("a", eventLog)
      instance1.increment("a", 3).await should be(3)
      instance1.value("a").await should be(3)

      new CappingService("b", eventLog).value("a").await should be(instance1.value("a").await)
    }
    "share the same value over 2 service-id instances for the same ids when the second is created and updated after the first" in {
      val instance1 = new CappingService("a", eventLog)
      instance1.increment("a", 3).await should be(3)
      instance1.value("a").await should be(3)

      val instance2 = new CappingService("b", eventLog)
      instance2.increment("b", 20).await should be(20)
      instance2.increment("b", -5).await should be(20)
      instance1.value("b").await should be(20)
    }
  }
}