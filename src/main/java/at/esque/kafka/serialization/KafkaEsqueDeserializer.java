package at.esque.kafka.serialization;

import at.esque.kafka.MessageType;
import at.esque.kafka.cluster.TopicMessageTypeConfig;
import at.esque.kafka.handlers.ConfigHandler;
import org.apache.avro.Schema;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public class KafkaEsqueDeserializer implements Deserializer<Object> {

    private ForgivingKafkaAvroDeserializer avroDeserializer;

    private Map<MessageType, Deserializer> deserializerMap = new EnumMap<MessageType, Deserializer>(MessageType.class) {{
        Arrays.stream(MessageType.values()).filter(type -> !type.equals(MessageType.AVRO)).forEach(type -> put(type, deserializerByType(type)));
    }};

    public KafkaEsqueDeserializer() {
        avroDeserializer = new ForgivingKafkaAvroDeserializer();
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
            return deserializerMap.get(isKey ? topicConfig.getKeyType() : topicConfig.getValueType()).deserialize(s, bytes);
        }
    }


    public void configure(Map<String, ?> configs, boolean isKey) {
        this.isKey = isKey;
        deserializerMap.values().forEach(deserializer -> deserializer.configure(configs, isKey));
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

    private Deserializer deserializerByType(MessageType type) {
        switch (type) {
            case STRING:
                return Serdes.String().deserializer();
            case SHORT:
                return Serdes.Short().deserializer();
            case INTEGER:
                return Serdes.Integer().deserializer();
            case LONG:
                return Serdes.Long().deserializer();
            case FLOAT:
                return Serdes.Float().deserializer();
            case DOUBLE:
                return Serdes.Double().deserializer();
            case BYTEARRAY:
                return Serdes.ByteArray().deserializer();
            case BYTEBUFFER:
                return Serdes.ByteBuffer().deserializer();
            case BYTES:
                return Serdes.Bytes().deserializer();
            case UUID:
                return Serdes.UUID().deserializer();
            case AVRO:
            case AVRO_TOPIC_RECORD_NAME_STRATEGY:
                return new ForgivingKafkaAvroDeserializer();
            default:
                throw new UnsupportedOperationException("no deserializer for Message type: " + type);
        }
    }
}

