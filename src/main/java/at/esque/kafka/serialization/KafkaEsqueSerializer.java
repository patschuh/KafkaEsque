package at.esque.kafka.serialization;

import at.esque.kafka.MessageType;
import at.esque.kafka.cluster.TopicMessageTypeConfig;
import at.esque.kafka.handlers.ConfigHandler;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.utils.Bytes;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class KafkaEsqueSerializer implements Serializer<Object> {

    private KafkaAvroSerializer avroSerialier = new KafkaAvroSerializer();

    private static final List<MessageType> AVRO_TYPES = Arrays.asList(MessageType.AVRO, MessageType.AVRO_TOPIC_RECORD_NAME_STRATEGY);

    private Map<MessageType, SerializerWrapper> serializerMap = new EnumMap<MessageType, SerializerWrapper>(MessageType.class) {{
        Arrays.stream(MessageType.values()).filter(type -> !AVRO_TYPES.contains(type)).forEach(type -> put(type, serializerByType(type)));
    }};

    private String clusterId;
    private boolean isKey;
    private ConfigHandler configHandler;

    public Object serialize(byte[] bytes) {
        throw new UnsupportedOperationException("Can't deserialize without topic name");
    }

    public byte[] serialize(String s, Object object) {
        TopicMessageTypeConfig topicConfig = configHandler.getConfigForTopic(clusterId, s);
        if (isKey ? AVRO_TYPES.contains(topicConfig.getKeyType()) : AVRO_TYPES.contains(topicConfig.getValueType())) {
            return avroSerialier.serialize(s, object);
        } else {
            SerializerWrapper serializerWrapper = serializerMap.get(isKey ? topicConfig.getKeyType() : topicConfig.getValueType());
            return serializerWrapper.serializer.serialize(s, serializerWrapper.function.apply(object));
        }
    }


    public void configure(Map<String, ?> configs, boolean isKey) {
        this.isKey = isKey;
        serializerMap.values().forEach(serializer -> serializer.serializer.configure(configs, isKey));
        if (configs.get("schema.registry.url") != null) {
            avroSerialier.configure(configs, isKey);
        }
        this.clusterId = (String) configs.get("kafkaesque.cluster.id");
        this.configHandler = (ConfigHandler) configs.get("kafkaesque.confighandler");
    }

    public void close() {
    }

    private SerializerWrapper serializerByType(MessageType type) {
        switch (type) {
            case STRING:
                return new SerializerWrapper<String>(s->s, Serdes.String().serializer());
            case SHORT:
                return new SerializerWrapper<Short>(Short::parseShort, Serdes.Short().serializer());
            case INTEGER:
                return new SerializerWrapper<Integer>(Integer::parseInt, Serdes.Integer().serializer());
            case LONG:
                return new SerializerWrapper<Long>(Long::parseLong, Serdes.Long().serializer());
            case FLOAT:
                return new SerializerWrapper<Float>(Float::parseFloat, Serdes.Float().serializer());
            case DOUBLE:
                return new SerializerWrapper<Double>(Double::parseDouble, Serdes.Double().serializer());
            case BYTEARRAY:
                return new SerializerWrapper<byte[]>(String::getBytes, Serdes.ByteArray().serializer());
            case BYTEBUFFER:
                return new SerializerWrapper<ByteBuffer>( s -> ByteBuffer.wrap(s.getBytes()), Serdes.ByteBuffer().serializer());
            case BYTES:
                return new SerializerWrapper<Bytes>(s -> Bytes.wrap(s.getBytes()), Serdes.Bytes().serializer());
            case UUID:
                return new SerializerWrapper<UUID>(UUID::fromString, Serdes.UUID().serializer());
            default:
                throw new UnsupportedOperationException("no serializer for Message type: " + type);
        }
    }
    public class SerializerWrapper<T>{
        private Function<String, T> function;
        private Serializer<T> serializer;

        public SerializerWrapper(Function<String, T> function, Serializer<T> serializer) {
            this.function = function;
            this.serializer = serializer;
        }
    }
}

