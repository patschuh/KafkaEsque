package at.esque.kafka.serialization;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForgivingKafkaAvroDeserializer extends KafkaAvroDeserializer {

    private static final Logger logger = LoggerFactory.getLogger(ForgivingKafkaAvroDeserializer.class);

    public Object deserialize(byte[] bytes) {
        try {
            return super.deserialize(bytes);
        } catch (Exception e) {
            logger.warn("Failed Avro deserialization", e);
        }
        return "Failed Avro deserialization! Content as String: " + new String(bytes);
    }

    public Object deserialize(String s, byte[] bytes) {
        try {
            return super.deserialize(bytes);
        } catch (Exception e) {
            logger.warn("Failed Avro deserialization in topic {}", s);
        }
        return "Failed Avro deserialization! Content as String: " + new String(bytes);
    }

    public Object deserialize(String s, byte[] bytes, Schema readerSchema) {
        try {
            return super.deserialize(bytes);
        } catch (Exception e) {
            logger.warn("Failed Avro deserialization in topic {}", s);
        }
        return "Failed Avro deserialization! Content as String: " + new String(bytes);
    }
}
