# KafkaEsque

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
