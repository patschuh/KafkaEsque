# KafkaEsque

## What's new

Starting with version 2.0.0 KafkaEsque uses JavaFX 17 instead of JavaFX 8, and provides prepackaged builds for most
operating systems (Windows, macOS, Linux) that were created with
[jpackage](https://docs.oracle.com/en/java/javase/17/jpackage/packaging-overview.html) using [GitHub Actions](actions)
(CI). Every release on GitHub can be tracked to its 'run' (one job for each OS) in the 'Gradle Build' workflow, and the
source code that was used to build it. Furthermore,
[jlink](https://docs.oracle.com/en/java/javase/17/docs/specs/man/jlink.html) bundles a minimal version of the JRE/JDK
with the required JavaFX libraries. Therefore, it is not necessary to have Java installed at all.

Thanks to the [badass-runtime plugin](https://badass-runtime-plugin.beryx.org/releases/latest/) for Gradle, the
application is still non-modular, and it was not necessary to use Java 9 modules (also known as Project Jigsaw).


## Usage

### Windows
On Windows you will most likely receive a smart screen warning because the app was not signed or uploaded to Microsoft
to verify it. You can safely ignore this warning. The app does not even require admin rights to install it because it is
being installed for the current user only.

### macOS
macOS has a similar problem with signing, except that while the dmg file that contains the app, displays the correct
message, and says it was signed by a non verified developer, the app inside the dmg does not. Instead, it says that the
app is damaged which is incorrect. If anyone knows how to fix this, please submit a pull request.

As most macOS user probably know, apps downloaded from the Internet are "in quarantine". You can see the flag
(`com.apple.quarantine`) yourself if you run, e.g., `ls -lh@` in the console. To install the app you have to remove the
flag first with, e.g., `xattr -rd com.apple.quarantine kafkaesque-2.0.0.dmg`.

### Linux
For Linux deb & rpm packages are provided. So every distribution that uses those, can install them. If someone actually
uses Kafkaesque on another Linux distribution, feel free to submit an issue, or even a PR, if you want up-to-date
packages for your distribution.

### JAR
You can still run just the JAR files as before version 2.0.0, but now they require Java 17 instead of Java 8.

:information_source: This is a JavaFX application and therefore requires OpenJFX.
* When using a JDK that includes OpenJFX simply run it with `java -jar KafkaEsque.jar`
* If OpenJFX is installed separately run it with `java -jar --module-path="path/to/openJfx/lib" --add-modules javafx.controls,javafx.fxml KafkaEsque.jar`

Alternatively you can start the JAR file with the provided helper script. Either with `./bin/KafkaEsque` on macOS or
Linux, or with `.bin/KafkaEsque.bat` on Windows.


## Features

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
Within the cluster.json file it is possible to configure Authentication for Kafka and Confluent Schema Registry:
Note: the secrets have to be given in the json file in plain text. This might be a security issue. Feel free to apply a PR if you want to improve this. 
sslEnabled controls the SSL Authentication method

Config of the Authentication can be done either in cluster.json directly or via the UI.

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

The http**s** and 'sslEnabled' is important if you want to use truststore and/or keystore otherwise those attributes are ignored and now sslContext is provided to Schema Registry client.

You can use only Basic Auth if youy SR is only protected with basic auth, you can use Token Auth if your SR is protected with an OAUTH Token, you can use only keystore+truststore if your SR is protected with mTLS or you can use both settings in parallel.
schemaRegistryBasicAuthUserInfo is deprecated since token auth is supported in addition to basic auth.
There is a schemaRegistryAuthMode property with possible values NONE, BASIC or TOKEN and schemaRegistryAuthConfig property with either basic auth credentials or OAuthToken.
 ```
	{
              ....
              "schemaRegistry": "https://myschemaregistry:8081", 
deprecated-> "schemaRegistryBasicAuthUserInfo": "<BasicAuthUser>:<BasicAuthPW>",
              "schemaRegistryAuthMode": "NONE|BASIC|TOKEN",
              "schemaRegistryAuthConfig": "<BasicAuthUser>:<BasicAuthPW>|<OAuthToken>:",
              ...
              "sslEnabled": true,
              "keyStoreLocation": "mykeystore.jks",
              "keyStorePassword": "mykeystorepw",
              "trustStoreLocation": "mytruststore.jks",
              "trustStorePassword": "mykeystorepw"
    }
 ```

###### Using SSL without domain name 
In some situation you might need to use ip address for your bootstrap server and SSL. 
With default config the API does a host name identification which fails in those scenarios with 
```
java.security.cert.CertificateException: No subject alternative names matching IP address .... found
```
If you select the toggle "No SSL Endpoint Identification" the kafka property "ssl.endpoint.identification.algorithm" and schema-registry property "schema-registry.ssl.endpoint.identification.algorithm"
are set to an empty string so that this identification is suppressed 

###### suppress cert path validation
In some situation you might need to suppress domain name validation for schema-registry.
With default config the API does a cert path validation which fails when using an ssh tunnel.
```
PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```
If you select the toggle "suppress Cert Path Validation" an empty trustmanager is set.

###### Settings

Check the settings.yaml in the <user.home>/.kafkaesque directory for cluster independent application settings

* use.system.menubar: use macOS System menu bar
  *  <span style="color:gray">default: true</span>
* trace.quick.select.enabled: enables quick select buttons in trace dialogs
  * <span style="color:gray">default: true</span>
* trace.quick.select.duration.list: configures the values of the quick select buttons as a comma separated list of [Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html) Strings
  * <span style="color:gray">default: PT2H, P1D, P7D</span>
![Trace Quick Select](https://kafka.esque.at/images/screenshots/quickselectTrace.png "Trace Quick Select")
* syntax.highlight.threshold.enabled: Enables the syntax highlight threshold, if enabled JSON syntax highlighting will be disabled for if the codearea contains more characters than the threshold <span style="color:gray">(syntax.highlight.threshold.characters)</span>.
  * <span style="color:gray">default: true</span>
* syntax.highlight.threshold.characters: configures the maximum number of characters allowed in a codeArea before syntax highlighting is disabled if the threshold is enabled <span style="color:gray">(syntax.highlight.threshold.enabled)</span>.
  * <span style="color:gray">default: 50000</span>
* default.key.messagetype: configures the default Key MessageType to use if no configuration was saved for the topic yet</span>.
  * <span style="color:gray">default: STRING</span>
* default.value.messagetype: configures the default Value MessageType to use if no configuration was saved for the topic yet</span>.
  * <span style="color:gray">default: STRING</span>
* check.for.updates.enabled: configures if KafkaEsque checks github-releases for a newer version on startup</span>.
  * <span style="color:gray">default: true</span>
* check.for.updates.duration.between.hours: configures how many hours a check of the latest version on github is valid before it is checked again</span>.
  * <span style="color:gray">default: 24</span>