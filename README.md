# KafkaEsque
:information_source: This is a JavaFX application and therfore requires OpenJFX to be installed when using OpenJDK
### Create, remove and describe Topics
![Create Topic Screenshot](https://kafka.esque.at/images/screenshots/CreateTopic.png "Create Topics")

![Describe Topic Screenshot](https://kafka.esque.at/images/screenshots/TopicDescription.png "Describe Topics")
***
### Publish Messages
![Publish Message Screenshot](https://kafka.esque.at/images/screenshots/PublishMessage.png "Publish Message Dialog")
***
### Consume Messages
***
### Trace Messages
#### By Key
Consumes Messages only keeping Messages in the result list where the message key matches the given key
#### in value
Consumes Messages only keeping Messages in the result list where a sequence in the message value matches the given regex
***
### Export Messages
Mesages displayed in the message list can be exportet in csv format and played into any cluster via a message book with minimal modifications.
***
### Topic Templates
Allows for defining and configurating topics once and apply them to different clusters with one file, see the [Wiki]("https://github.com/patschuh/KafkaEsque/wiki/Topic-Templates") for Details
***
### Message Books
Allows for playing a set of Messages over different topics into a cluster, see the [Wiki]("https://github.com/patschuh/KafkaEsque/wiki/Message-Books") for details
***

### Authentication 
At the moment the UI only supports cluster configs without any authentication
Within the cluster.json file it is possible to configure Authentication for Kafka and Confluent Schema Registry:
Note: the secrets have to be given in the json file in plain text. This might be a security issue. Feel free to apply a PR if you want to improve this. 

sslEnabled controls the SSL Authentication method

###### Example for SSL with mTLS Authentication to the broker: 
 ```
	{
		"identifier": "my-mtls-secured-cluster",
		"bootstrapServers": "broker:<portofmtlslistener>",
		"sslEnabled": true,
		"keyStoreLocation": "mykeystore.jks",
		"keyStorePassword": "mykeystorepw",
		"trustStoreLocation": "mytruststore.jks",
		"trustStorePassword": "mykeystorepw"
    }
 ```
###### Example for SASL_SSL Authentication

saslSecurityProtocol,saslMechanism and saslJaasConfig can be provided
This can also be combined with given trust and keystore configuration

 ```
	{
		"identifier": "my-mtls-secured-cluster",
		"bootstrapServers": "broker:<portofmtlslistener>",
        "saslSecurityProtocol": "SASL_SSL",
        "saslMechanism"  : "PLAIN", 
        "saslJaasConfig" : "org.apache.kafka.common.security.plain.PlainLoginModule required serviceName=kafka username=\"MYUSER\" password=\"53CR37\";"
    }
 ```

###### Example with Schema Registry with HTTPS and Basic Auth 

The http**s** and 'sslEnabled' is important if you want to use truststore and/or keystore otherwise those attributes are ignored and now sslContext is provided to Schema Registry client

you can use only Basic Auth if you SR is only protected with basic auth, you can use only keystore+truststore if your SR is protected with mTLS or you can use both settings in parallel. 

 ```
	{
		....
		"schemaRegistry": "https://myschemaregistry:8081", 
        "schemaRegistryBasicAuthUserInfo": "<BasicAuthUser>:<BasicAuthPW>",
        ...
		"sslEnabled": true,
		"keyStoreLocation": "mykeystore.jks",
		"keyStorePassword": "mykeystorepw",
		"trustStoreLocation": "mytruststore.jks",
		"trustStorePassword": "mykeystorepw"
    }
 ```

