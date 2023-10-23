package at.esque.kafka.serialization;

import com.google.protobuf.Message;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ForgivingProtobufAvroDeserializer implements Deserializer<Object> {

    private KafkaProtobufDeserializer<Message> baseDeserializer = new KafkaProtobufDeserializer<>();
    private static final Logger logger = LoggerFactory.getLogger(ForgivingProtobufAvroDeserializer.class);

    public Object deserialize(byte[] bytes) {
        throw new UnsupportedOperationException();
    }

    public Object deserialize(String s, byte[] bytes) {
        try {
            return baseDeserializer.deserialize(s, bytes);
        } catch (Exception e) {
            logger.warn("Failed Protobuf deserialization in topic {}", s);
        }
        return "Failed Protobuf deserialization! Content as String: " + new String(bytes);
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        baseDeserializer.configure(configs, isKey);
    }
}
