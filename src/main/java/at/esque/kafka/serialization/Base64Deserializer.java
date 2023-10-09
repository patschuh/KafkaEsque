package at.esque.kafka.serialization;

import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Base64;

public class Base64Deserializer extends StringDeserializer {

    @Override
    public String deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        } else {
            return new String(Base64.getEncoder().encode(data));
        }
    }
}

