package at.esque.kafka.serialization.jackson;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class KafkaHeaderDeserializer extends StdDeserializer<Header> {


    public KafkaHeaderDeserializer() {
        this(null);
    }

    public KafkaHeaderDeserializer(Class<RecordHeader> clazz) {
        super(clazz);
    }

    @Override
    public Header deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
        String key = jsonNode.get("key").asText();
        String value = jsonNode.get("value").asText();

        return new RecordHeader(key, value == null ? null : value.getBytes(StandardCharsets.UTF_8));
    }


}
