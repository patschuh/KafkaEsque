package at.esque.kafka.serialization;

import at.esque.kafka.MessageType;
import at.esque.kafka.cluster.TopicMessageTypeConfig;
import at.esque.kafka.handlers.ConfigHandler;
import org.apache.avro.Schema;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Map;

public class KafkaEsqueDeserializer implements Deserializer<Object> {

    private ForgivingKafkaAvroDeserializer avroDeserializer;
    private StringDeserializer stringDeserializer;

    public KafkaEsqueDeserializer() {
        avroDeserializer = new ForgivingKafkaAvroDeserializer();
        stringDeserializer = new StringDeserializer();
    }

    private String clusterId;
    private boolean isKey;
    private ConfigHandler configHandler;

    public Object deserialize(byte[] bytes) {
        throw new UnsupportedOperationException("Can't deserialize without topic name");
    }

    public Object deserialize(String s, byte[] bytes) {
        TopicMessageTypeConfig topicConfig = configHandler.getConfigForTopic(clusterId, s);
        if (isKey ? topicConfig.getKeyType().equals(MessageType.AVRO) : topicConfig.getValueType().equals(MessageType.AVRO)) {
            return avroDeserializer.deserialize(bytes);
        } else {
            return stringDeserializer.deserialize(s, bytes);
        }
    }


    public void configure(Map<String, ?> configs, boolean isKey) {
        this.isKey = isKey;
        stringDeserializer.configure(configs, isKey);
        if (configs.get("schema.registry.url") != null) {
            avroDeserializer.configure(configs, isKey);
        }
        this.clusterId = (String) configs.get("kafkaesque.cluster.id");
        this.configHandler = (ConfigHandler) configs.get("kafkaesque.confighandler");
    }

    public Object deserialize(String s, byte[] bytes, Schema readerSchema) {
        return this.deserialize(s, bytes);
    }

    public void close() {
    }
}

