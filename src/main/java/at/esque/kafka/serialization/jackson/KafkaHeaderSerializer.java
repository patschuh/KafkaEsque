package at.esque.kafka.serialization.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.kafka.common.header.Header;

import java.io.IOException;

public class KafkaHeaderSerializer extends StdSerializer<Header> {


    public KafkaHeaderSerializer() {
        this(null);
    }

    public KafkaHeaderSerializer(Class<Header> clazz) {
        super(clazz);
    }

    @Override
    public void serialize(Header header, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("key", header.key());
        jsonGenerator.writeStringField("value", header.value() == null ? null : new String(header.value()));
        jsonGenerator.writeEndObject();
    }
}
