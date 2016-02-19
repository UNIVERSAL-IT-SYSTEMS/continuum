# Continuum
## continuum-kafka
A Kafka Akka Extension - abstracts Kafka publisher and consumer APIs. Supports cluster data distribution and decoupling in the steam

### Basic setup

    object NodeA extends App { 
      val system: ActorSystem = ...//manage lifecycle..  
    }  
    
    val kafka = Kafka(system) 

Basic source

    kafka log data  
   
Basic sink (consumers)
          
          val receivers = List(subscriberA, subscriberB)
          kafka.sink(topics, receivers)
          
### Send To Kafka Using The Default Source (Producer)      
Simply send data to Kafka based on application load-time configuration (from deploy env/config)
which populates the default producer:

Where data is a simple json String and to is a topic:
      
      kafka.log(data, to)
      
Using the SourceEvent directly, again with the default source. The SourceEvent provides
meta-data on the event type hint, a nonce from an MD5 hash and the UTC timestamp of ingestion/creation. 
This allows traceability throughout the system. However, the meta-data is not yet serialized to Kafka - Roadmap.

      val etype = classOf[YourEventType]
      val data: Array[Byte] = yourDataBytes
      
      import com.tuplejump.continuum.ClusterProtocol._
      val data = SourceEvent(etype, data, to) 
      kafka log data  
     
### Access Default Source (Producer) Directly

      val source = kafka.source    
          
### Create A New Source with Additional and/or Overriding Configuration

    val additional = Map(...configs..)
    val source = kafka.source(additional)
    
    def inboundStream[A: ClassTag](data: A): Unit = {
      val bytes = data.toBytes
      source ! SourceEvent(classOf[A], data, to)
    }

### Create A Sink (Consumer)    
Start by creating your receivers:
   
    import com.tuplejump.continuum.ClusterProtocol.SinkEvent
    
    class SimpleSubscriberA extends Actor { 
       def receive: Actor.Receive = { 
         case SinkEvent(_, data, _, topic,_) => process(data)
       } 
       
       def process(data: Array[Byte]): Unit = { ... } 
    }

Pass them into the new Sink, one or more:

    val receivers = List(subscriberA, subscriberB)
    kafka.sink(topics, receivers)
    
Just use for subscribing to streams:
    
    Kafka(actorSystem).sink(topics, receivers)
  