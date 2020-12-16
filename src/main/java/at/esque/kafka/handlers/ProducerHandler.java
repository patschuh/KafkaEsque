package at.esque.kafka.handlers;

import at.esque.kafka.MessageType;
import at.esque.kafka.cluster.ClusterConfig;
import at.esque.kafka.cluster.SslSocketFactoryCreator;
import at.esque.kafka.cluster.TopicMessageTypeConfig;
import at.esque.kafka.serialization.ExtendedJsonDecoder;
import at.esque.kafka.serialization.KafkaEsqueSerializer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.client.rest.entities.Schema;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Singleton
public class ProducerHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerHandler.class);

    @Inject
    private ConfigHandler configHandler;

    private Map<UUID, ProducerWrapper> registeredProducers = new ConcurrentHashMap<>();

    public ProducerHandler() {
    }

    public Optional<ProducerWrapper> getProducer(UUID producerId) {
        return Optional.ofNullable(registeredProducers.get(producerId));
    }

    public Map<UUID, ProducerWrapper> getRegisteredProducers() {
        return registeredProducers;
    }

    public void setRegisteredProducers(Map<UUID, ProducerWrapper> registeredProducers) {
        this.registeredProducers = registeredProducers;
    }

    public UUID registerProducer(ClusterConfig clusterConfig) throws IOException {
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, clusterConfig.getBootStrapServers());
        UUID producerId = UUID.randomUUID();
        RestService schemaRegistryRestService = null;
        props.setProperty(ProducerConfig.CLIENT_ID_CONFIG, "kafkaesque-" + producerId);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaEsqueSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaEsqueSerializer.class);
        props.setProperty("auto.register.schemas", "false");
        props.setProperty("kafkaesque.cluster.id", clusterConfig.getIdentifier());
        props.put("kafkaesque.confighandler", configHandler);
        if (StringUtils.isNotEmpty(clusterConfig.getSchemaRegistry())) {
            props.setProperty("schema.registry.url", clusterConfig.getSchemaRegistry());
            props.putAll(configHandler.getSchemaRegistryAuthProperties(clusterConfig));
            schemaRegistryRestService = new RestService(clusterConfig.getSchemaRegistry());

            schemaRegistryRestService.configure(configHandler.getSchemaRegistryAuthProperties(clusterConfig));

            if (clusterConfig.isSchemaRegistryHttps())
            {
                SSLSocketFactory sslSocketFactory = SslSocketFactoryCreator.buildSSlSocketFactory(clusterConfig);
                schemaRegistryRestService.setSslSocketFactory(sslSocketFactory);
            }
        }

        props.putAll(configHandler.getSslProperties(clusterConfig));
        props.putAll(configHandler.getSaslProperties(clusterConfig));
        props.putAll(configHandler.readProducerConfigs(clusterConfig.getIdentifier()));

        LOGGER.info("Creating new Producer with properties: [{}]", props);
        registeredProducers.put(producerId, new ProducerWrapper(clusterConfig.getIdentifier(), new KafkaProducer(props), schemaRegistryRestService));
        return producerId;
    }

    public void deregisterProducer(UUID producerId) {
        ProducerWrapper deregisteredProducer = registeredProducers.get(producerId);
        deregisteredProducer.getProducer().close();
        registeredProducers.remove(producerId);
        LOGGER.info("Deregistered producer with id [{}]", producerId);
    }

    public void sendMessage(UUID producerId, String topic, Integer selectedpartition, String key, String value) throws InterruptedException, ExecutionException, TimeoutException, IOException, RestClientException {
        sendMessage(producerId, topic, selectedpartition, key, value, null);
    }

    public RecordMetadata sendMessage(UUID producerId, String topic, Integer selectedpartition, String key, String value, List<Header> headers) throws InterruptedException, ExecutionException, TimeoutException, IOException, RestClientException {
        ProducerWrapper producerWrapper = registeredProducers.get(producerId);
        if (producerWrapper == null) {
            throw new RuntimeException(String.format("Producer with id [%s] does not exist!", producerId));
        }
        TopicMessageTypeConfig typeConfig = configHandler.getConfigForTopic(producerWrapper.getClusterId(), topic);
        ProducerRecord record;
        Object keyValue = typeConfig.getKeyType() == MessageType.AVRO ? createRecord(producerWrapper, key, topic, true) : key;
        Object valueValue = typeConfig.getValueType() == MessageType.AVRO ? createRecord(producerWrapper, value, topic, false) : value;
        if (selectedpartition != null && selectedpartition > -1) {
            record = new ProducerRecord(topic, selectedpartition, keyValue, valueValue);
        } else {
            record = new ProducerRecord(topic, keyValue, valueValue);
        }
        if (headers != null) {
            headers.forEach(header -> record.headers().add(header));
        }
        return publishRecord(producerWrapper, record);
    }

    public RecordMetadata sendRecord(UUID producerId, ProducerRecord producerRecord) throws InterruptedException, ExecutionException, TimeoutException {
        return publishRecord(registeredProducers.get(producerId), producerRecord);
    }

    private RecordMetadata publishRecord(ProducerWrapper producerWrapper, ProducerRecord record) throws InterruptedException, ExecutionException, TimeoutException {
        Future<RecordMetadata> future = producerWrapper.getProducer().send(record);
        RecordMetadata metadata = future.get(1, TimeUnit.MINUTES);
        LOGGER.debug(String.format("topic [%s] / partition [%s] / offset [%s]", metadata.topic(), metadata.partition(), metadata.offset()));
        return metadata;
    }

    private GenericRecord createRecord(ProducerWrapper producerWrapper, String json, String topic, boolean isKey) throws IOException, RestClientException {
        if (json == null) {
            return null;
        }
        Schema schema = getSchemaFromRegistry(producerWrapper.getSchemaRegistryRestService(), topic + (isKey ? "-key" : "-value"));
        org.apache.avro.Schema avroSchema = new org.apache.avro.Schema.Parser().parse(schema.getSchema());

        Decoder jsonDecoder = new ExtendedJsonDecoder(avroSchema, json);
        DatumReader<GenericRecord> reader = new GenericDatumReader<>(avroSchema);

        return reader.read(null, jsonDecoder);
    }

    private Schema getSchemaFromRegistry(RestService schemaRegistryRestService, String subject) throws IOException, RestClientException {
        return schemaRegistryRestService.getLatestVersion(subject);
    }
}
