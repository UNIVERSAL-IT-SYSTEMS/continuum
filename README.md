# Continuum [![Build Status](http://travis-ci.org/tuplejump/continuum.svg)](http://travis-ci.org/tuplejump/continuum)
Description and documentation coming. The project is built on [Akka](http://github.com/akka/akka), leverages
[Eventuate](http://github.com/RBMHTechnology/eventuate) for the 
core artifact, and [Apache Kafka](https://github.com/apache/kafka) v0.9.0.x in the cluster artifact. 
    
This project is separated into a few modules you can chose from:
## continuum-kafka
A Kafka Akka Extension - abstracts Kafka publisher and consumer APIs. Supports cluster data distribution and decoupling in the steam
For more information and examples see [continuum-kafka/README.md](continuum-kafka/README.md).

### Getting Started

    object NodeA extends App { 
      val system: ActorSystem = ...//manage lifecycle..  
    }  

Use anywhere in your Akka app:
    
    val kafka = Kafka(system) 

Basic source usages

    kafka log data  
or 
    
    kafka.source ! SourceEvent()
 
Basic sink (consumers)
   
    import com.tuplejump.continuum.ClusterProtocol.SinkEvent
        
    class SimpleSubscriberA extends Actor { 
      def receive: Actor.Receive = { 
        case SinkEvent(_, data, _, topic,_) => process(data)
      } 
           
      def process(data: Array[Byte]): Unit = { ... } 
    }
  
Start consuming data:
    
    val receivers = List(subscriberA, subscriberB)
    Kafka(system).sink(topics, receivers)

See [continuum-kafka/README.md](continuum-kafka/README.md)
          
## continuum-common
Simple CQRS pattern support. Work in progress.

## continuum-core
Leveraging Eventuate for CRDT support and in-memory use cases in distributed systems. Work in progress.

## Build and Run

    git clone git@github.com:tuplejump/continuum.git
    cd continuum

Continuum requires Scala 2.11.x and JDK 1.8. Updating soon to support JDK 1.7.
 
### continuum-examples  
See [continuum-examples/README.md](continuum-examples/README.md) for information on running the examples.
        
### Running Tests
This is a WIP, very few tests are pushed yet but the standard is:

    sbt test
    
### Running IT (Integration) Tests
You can run all IT tests with

    sbt it:test
     
To run modules explicitly:

    sbt core/it:test
    sbt kafka/it:test
     
Kafka IT tests use [embedded-kafka](http://github.com/tuplejump/embedded-kafka). 
If you are running Zookeeper and/or Kafka locally, unless they are on different ports you will need to stop them first.
This lets you only run those:

    sbt kafka/it:test
 
## Artifacts
This is a new project and not yet published to an artifact repo. Until then you do have
to build and publish the artifacts you want. For example:

    sbt kafka/publish-local

## Roadmap

- Optionally Implicit Serialization
- Configurable serialization providers
- Simplification of the Framework APIs
- Tests!
- Better types
- More config options 
- Security
- Much more integration with Eventuate
- More features for Kafka
- General cleanup - this is a new project      