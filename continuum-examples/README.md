# Continuum Examples 
## continuum-kafka   
The Kafka examples require one or a cluster of Zookeeper/Kafka servers running to connect to. 
To start local instances with defaults:

        ./bin/zookeeper-server-start.sh config/zookeeper.properties
        ./bin/kafka-server-start.sh config/server.properties

### Prerequisites
You must either have a local Zookeeper and Kafka server
running, or configure the necessary connection configurations to connect to
your remote cluster. If no brokers are available, ZK/Kafka will throw java.net.ConnectException :)

### Configuration
For remote cluster: (typically these are read from the deploy environment,
variables set by chef, for instance, on deploy). The config file is merely
for fallbacks, defaults, test deploys etc.

    #initial seed:
    continuum.kafka.port = 9092
    continuum.kafka.host.name = "127.0.0.1"
    
    # Comma-separated list of kafka 'host:port' entries
    continuum.kafka.connect = "127.0.0.1:9092"
    
    # Comma-separated list of zookeeper 'host:port' entries
    continuum.kafka.zookeeper.connect = "127.0.0.1:2181"
 
### Run 
Run the initial, simple sample and select examples.KafkaExampleSimple from the list:
      
    sbt examples/run
 