package at.esque.kafka.serialization;

import at.esque.kafka.MessageType;
import at.esque.kafka.cluster.TopicMessageTypeConfig;
import at.esque.kafka.handlers.ConfigHandler;
import org.apache.avro.generic.GenericData;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public class KafkaEsqueDeserializer implements Deserializer<Object> {

    private Map<MessageType, Deserializer> deserializerMap = new EnumMap<MessageType, Deserializer>(MessageType.class) {{
        Arrays.stream(MessageType.values()).forEach(type -> put(type, deserializerByType(type)));
    }};

    public KafkaEsqueDeserializer() {
    }

    private String clusterId;
    private boolean isKey;
    private ConfigHandler configHandler;

    public Object deserialize(byte[] bytes) {
        throw new UnsupportedOperationException("Can't deserialize without topic name");
    }

    public Object deserialize(String s, byte[] bytes) {
        TopicMessageTypeConfig topicConfig = configHandler.getConfigForTopic(clusterId, s);
        MessageType messageType = (isKey ? topicConfig.getKeyType() : topicConfig.getValueType());
        Object deserializedObj = deserializerMap.get(messageType).deserialize(s, bytes);

        Integer schemaId = null;

        if (isSchemaRegistryMessageType(messageType)) {
            schemaId = getSchemaId(bytes);
        }
        if ((deserializedObj instanceof GenericData.Record)) {
            GenericData.Record rec = (GenericData.Record) deserializedObj;
            rec.getSchema().addProp("schema-registry-schema-id", schemaId);
        }


        return deserializedObj;
    }

    private static boolean isSchemaRegistryMessageType(MessageType messageType) {
        return MessageType.AVRO.equals(messageType)
                || MessageType.AVRO_TOPIC_RECORD_NAME_STRATEGY.equals(messageType)
                || MessageType.PROTOBUF_SR.equals(messageType);
    }


    public void configure(Map<String, ?> configs, boolean isKey) {
        this.isKey = isKey;
        deserializerMap.values().forEach(deserializer -> {
            if ((deserializer instanceof ForgivingKafkaAvroDeserializer || deserializer instanceof ForgivingProtobufAvroDeserializer) && configs.get("schema.registry.url") == null) {
                //Don't call configure for the AvroDeserializer if there is no schema registry url to prevent exception, in cases where avro is not even used
            } else {
                deserializer.configure(configs, isKey);
            }
        });
        this.clusterId = (String) configs.get("kafkaesque.cluster.id");
        this.configHandler = (ConfigHandler) configs.get("kafkaesque.confighandler");
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
            case BASE64:
                return new Base64Deserializer();
            case UUID:
                return Serdes.UUID().deserializer();
            case PROTOBUF_SR:
                return new ForgivingProtobufAvroDeserializer();
            case AVRO:
            case AVRO_TOPIC_RECORD_NAME_STRATEGY:
                return new ForgivingKafkaAvroDeserializer();
            default:
                throw new UnsupportedOperationException("no deserializer for Message type: " + type);
        }
    }

    protected Integer getSchemaId(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        if (buffer.get() != 0x0) {
            return -1;
        }

        return buffer.getInt();
    }
}

