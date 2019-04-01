package at.esque.kafka.serialization;

import at.esque.kafka.MessageType;
import at.esque.kafka.cluster.TopicMessageTypeConfig;
import at.esque.kafka.handlers.ConfigHandler;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Map;

public class KafkaEsqueSerializer implements Serializer<Object> {

    private KafkaAvroSerializer avroSerialier = new KafkaAvroSerializer();
    private StringSerializer stringSerializer = new StringSerializer();

    private String clusterId;
    private boolean isKey;
    private ConfigHandler configHandler;

    public Object serialize(byte[] bytes) {
        throw new UnsupportedOperationException("Can't deserialize without topic name");
    }

    public byte[] serialize(String s, Object object) {
        TopicMessageTypeConfig topicConfig = configHandler.getConfigForTopic(clusterId, s);
        if (isKey ? topicConfig.getKeyType().equals(MessageType.AVRO) : topicConfig.getValueType().equals(MessageType.AVRO)) {
            return avroSerialier.serialize(s, object);
        } else {
            return stringSerializer.serialize(s, (String) object);
        }
    }


    public void configure(Map<String, ?> configs, boolean isKey) {
        this.isKey = isKey;
        stringSerializer.configure(configs, isKey);
        if (configs.get("schema.registry.url") != null) {
            avroSerialier.configure(configs, isKey);
        }
        this.clusterId = (String) configs.get("kafkaesque.cluster.id");
        this.configHandler = (ConfigHandler) configs.get("kafkaesque.confighandler");
    }

    public void close() {
    }
}

