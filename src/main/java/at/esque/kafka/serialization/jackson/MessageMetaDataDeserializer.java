package at.esque.kafka.serialization.jackson;

import at.esque.kafka.topics.metadata.MessageMetaData;
import at.esque.kafka.topics.metadata.NumericMetadata;
import at.esque.kafka.topics.metadata.StringMetadata;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class MessageMetaDataDeserializer extends StdDeserializer<MessageMetaData> {

    public MessageMetaDataDeserializer() {
        this(null);
    }

    public MessageMetaDataDeserializer(Class<MessageMetaData> clazz) {
        super(clazz);
    }

    @Override
    public MessageMetaData deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
        String name = jsonNode.get("name").asText();
        JsonNode value = jsonNode.get("value");

        if (value.isNumber()) {
            return new NumericMetadata(name, value.asLong());
        } else {
            return new StringMetadata(name, value.asText());
        }
    }
}
