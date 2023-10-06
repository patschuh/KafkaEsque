package at.esque.kafka.serialization;

import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Base64;

public class Base64Serializer extends StringSerializer {

    @Override
    public byte[] serialize(String topic, String data) {
        if (data == null) {
            return new byte[0];
        } else {
            return Base64.getDecoder().decode(data);
        }
    }
}
