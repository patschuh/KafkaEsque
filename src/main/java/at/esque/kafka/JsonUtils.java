package at.esque.kafka;

import at.esque.kafka.controls.JsonTreeItem;
import at.esque.kafka.serialization.jackson.KafkaHeaderDeserializer;
import at.esque.kafka.serialization.jackson.KafkaHeaderSerializer;
import at.esque.kafka.serialization.jackson.MessageMetaDataDeserializer;
import at.esque.kafka.topics.KafkaMessage;
import at.esque.kafka.topics.metadata.MessageMetaData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import io.netty.util.internal.StringUtil;
import javafx.scene.control.TreeItem;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class JsonUtils {

    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    private JsonUtils() {
    }

    private static final ObjectMapper objectMapper = initializeObjectMapper();

    private static ObjectMapper initializeObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(Header.class, new KafkaHeaderDeserializer());
        simpleModule.addDeserializer(MessageMetaData.class, new MessageMetaDataDeserializer());
        simpleModule.addSerializer(RecordHeader.class, new KafkaHeaderSerializer());
        objectMapper.registerModule(simpleModule);
        return objectMapper;
    }

    public static void writeMessageToJsonFile(List<KafkaMessage> messages, Writer writer) {
        try {
            List<JsonNode> value1 = messages.stream()
                    .map(kafkaMessage -> {
                                JsonNode jsonNode = objectMapper.valueToTree(kafkaMessage);
                                String value = kafkaMessage.getValue();
                                String key = kafkaMessage.getKey();
                                try {
                                    JsonNode jsonNode1 = objectMapper.readTree(value);
                                    if (jsonNode1.isObject()) {
                                        ((ObjectNode) jsonNode).set("value", jsonNode1);
                                    }
                                } catch (Exception e) {
                                    logger.warn("Failed to convert value to jsonNode [{}]", value);
                                }
                                try {
                                    JsonNode jsonNode1 = objectMapper.readTree(key);
                                    if (jsonNode1.isObject()) {
                                        ((ObjectNode) jsonNode).set("key", jsonNode1);
                                    }
                                } catch (Exception e) {
                                    logger.warn("Failed to convert key to jsonNode [{}]", key);
                                }
                                return jsonNode;
                            }
                    ).toList();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, value1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<KafkaMessage> readMessages(Reader reader) {
        ArrayList<KafkaMessage> kafkaMessages = new ArrayList<>();
        try {
            JsonNode inputMessages = objectMapper.readTree(reader);
            if (inputMessages.isArray()) {
                inputMessages.iterator().forEachRemaining(singleMessage -> {
                    JsonNode value = singleMessage.get("value");
                    JsonNode key = singleMessage.get("key");

                    if (value.isObject()) {
                        try {
                            ((ObjectNode) singleMessage).set("value", TextNode.valueOf(objectMapper.writeValueAsString(value)));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    if (key.isObject()) {
                        try {
                            ((ObjectNode) singleMessage).set("key", TextNode.valueOf(objectMapper.writeValueAsString(key)));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    try {
                        KafkaMessage kafkaMessage = objectMapper.readValue(singleMessage.toString(), KafkaMessage.class);
                        kafkaMessages.add(kafkaMessage);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return kafkaMessages;
    }

    public static String formatJson(String string) {
        try {
            Object jsonObject = string == null ? null : objectMapper.readValue(string, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        } catch (Exception e) {
            return String.format("%s%s[Formatting Error: %s]", string, System.lineSeparator(), e.getMessage());
        }
    }

    public static String formatJson(Collection<JsonNode> jsonNodes) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNodes);
        } catch (Exception e) {
            return String.format("%s%s[Formatting Error: %s]", jsonNodes, System.lineSeparator(), e.getMessage());
        }
    }

    public static String formatJson(JsonNode jsonNode) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (Exception e) {
            return String.format("%s%s[Formatting Error: %s]", jsonNode, System.lineSeparator(), e.getMessage());
        }
    }

    public static ValidationResult validate(String jsonInString) {
        if (jsonInString == null) {
            return new ValidationResult(true, true);
        }

        try {
            objectMapper.readTree(jsonInString);
            return new ValidationResult(true, jsonInString.isBlank());
        } catch (IOException e) {
            return new ValidationResult(false, jsonInString.isBlank(), e.getMessage());
        }
    }


    public static JsonTreeItem buildPatchTreeFromJson(String value, ArrayNode patch) throws IOException {
        final JsonTreeItem jsonTreeItem = buildTreeFromJson(value);

        patch.forEach(jsonNode -> {
            applyPatch(jsonTreeItem, jsonNode);
        });

        applyChangeTypeIfChildHasChangeTypes(jsonTreeItem, "replace");

        return jsonTreeItem;
    }

    private static void applyPatch(JsonTreeItem jsonTree, JsonNode jsonNode) {
        if ("add".equals(jsonNode.get("op").textValue())) {
            logger.info("need to do add for {}:\n {}", jsonNode.get("path").textValue(), jsonNode.get("value").toPrettyString());

            final JsonNode value = jsonNode.get("value");
            final String key = jsonNode.get("path").textValue();
            final JsonTreeItem childWithPath = findChildWithPath(jsonTree, key.substring(0, key.lastIndexOf("/")));
            final JsonTreeItem childNode = new JsonTreeItem(key.substring(key.lastIndexOf("/") + 1), "");
            addNode(childWithPath, childNode, value);
        }
        if ("remove".equals(jsonNode.get("op").textValue())) {
            logger.info("need to do remove for {}:\n {}", jsonNode.get("path").textValue(), jsonNode.get("value").toPrettyString());
            final JsonTreeItem path = findChildWithPath(jsonTree, jsonNode.get("path").textValue());
            if (path != null) {
                removeNode(path, jsonNode);
            } else {
                logger.error("could not find node :(");
            }
        }
        if ("replace".equals(jsonNode.get("op").textValue())) {
            logger.info("need to do replace for {}:\n {}", jsonNode.get("path").textValue(), jsonNode.get("value").toPrettyString());
            final JsonTreeItem path = findChildWithPath(jsonTree, jsonNode.get("path").textValue());
            if (path != null) {
                replaceValue(path, jsonNode);
            } else {
                logger.error("could not find node :(");
            }
        }
    }

    private static void addNode(JsonTreeItem childWithPath, JsonTreeItem childNode, JsonNode value) {
        childWithPath.getChildren().add(childNode);
        childNode.setPropertyChangedType("add");
        childNode.setPropertyValue("added");
        applyCorrectAdder(value, childNode);
        applyChangeTypeAndPropagateToChilds(childNode, "add");
    }

    private static void applyChangeTypeAndPropagateToChilds(JsonTreeItem childNode, String changeType) {
        childNode.setPropertyChangedType(changeType);
        childNode.getChildren().forEach(child -> {
            if (child instanceof JsonTreeItem) {
                applyChangeTypeAndPropagateToChilds((JsonTreeItem) child, changeType);
            }
        });
    }


    private static boolean applyChangeTypeIfChildHasChangeTypes(JsonTreeItem root, String changeType) {
        AtomicBoolean childHasChangeType = new AtomicBoolean(false);
        root.getChildren().forEach(child -> {
            if (child instanceof JsonTreeItem && applyChangeTypeIfChildHasChangeTypes((JsonTreeItem) child, changeType)) {
                childHasChangeType.set(true);
            }
        });
        if (childHasChangeType.get() && root.getPropertyChangedType() == null) {
            root.setPropertyChangedType(changeType);
        }
        return root.getPropertyChangedType() != null;
    }

    private static void removeNode(JsonTreeItem path, JsonNode jsonNode) {
        path.setPropertyName(" -" + path.getPropertyName() + "-");
        path.setPropertyValue("removed" + (path.getPropertyValue() == null ? "" : path.getPropertyValue()));
        applyChangeTypeAndPropagateToChilds(path, jsonNode.get("op").textValue());
        AtomicBoolean reindex = new AtomicBoolean(false);
        path.getParent().getChildren().forEach(sibling -> {
            if (sibling instanceof JsonTreeItem) {
                final JsonTreeItem castSibling = (JsonTreeItem) sibling;
                if (reindex.get()) {
                    try {
                        long l = Long.parseLong(castSibling.getPropertyName());
                        logger.info("reindexing");
                        if (castSibling.getPropertyValue() == null) {
                            castSibling.setPropertyValue(" was index " + l + " before");
                        }
                        castSibling.setPropertyName((--l) + "");
                    } catch (NumberFormatException e) {
                        logger.warn("could not parse {} as long (path = {})", castSibling.getPropertyName(), ((JsonTreeItem) sibling).getPath());
                    }
                }
                if (path.equals(sibling)) {
                    reindex.set(true);
                }
            }
        });
    }

    private static void replaceValue(JsonTreeItem path, JsonNode jsonNode) {
        if (jsonNode.get("value").isNull()) {
            path.setPropertyValue((path.getPropertyValue() == null ? "" : path.getPropertyValue()) + " -> " + ((jsonNode.get("value") instanceof TextNode) ? jsonNode.get("value").textValue() : jsonNode.get("value")));
            applyChangeTypeAndPropagateToChilds(path, "remove");
        } else {
            path.setPropertyValue(path.getPropertyValue() + " -> " + ((jsonNode.get("value") instanceof TextNode) ? jsonNode.get("value").textValue() : jsonNode.get("value")));
            path.setPropertyChangedType(jsonNode.get("op").textValue());
        }
    }


    private static JsonTreeItem findChildWithPath(JsonTreeItem root, String substring) {
        if (root.getPath().equals(substring)) {
            return root;
        }
        return findChildWithPath(root.getChildren(), substring);
    }

    private static JsonTreeItem findChildWithPath(List<TreeItem<String>> list, String substring) {
        for (TreeItem<String> item : list) {
            if (item instanceof JsonTreeItem) {
                final JsonTreeItem childWithPath = findChildWithPath((JsonTreeItem) item, substring);
                if (childWithPath != null) {
                    return childWithPath;
                }
            }
        }
        return null;
    }


    public static JsonTreeItem buildTreeFromJson(String value) throws IOException {
        JsonNode json = objectMapper.readTree(value);
        if (json == null) {
            return new JsonTreeItem(null, null);
        }
        JsonTreeItem root = new JsonTreeItem("root", null);
        root.setExpanded(true);
        if (json instanceof ArrayNode) {
            recursivelyAddElements((ArrayNode) json, root);
        } else if (json instanceof TextNode) {
            recursivelyAddElements((TextNode) json, root);
        } else {
            recursivelyAddElements(json, root);
        }
        return root;
    }


    public static JsonNode readStringAsNode(String value) throws IOException {
        return objectMapper.readTree(value);
    }

    private static void recursivelyAddElements(JsonNode jsonNode, JsonTreeItem treeItem) {
        jsonNode.fields().forEachRemaining(stringJsonNodeEntry -> {
            JsonTreeItem newItem = new JsonTreeItem(stringJsonNodeEntry.getKey(), null);
            treeItem.getChildren().add(newItem);
            applyCorrectAdder(stringJsonNodeEntry.getValue(), newItem);
        });
    }

    private static void recursivelyAddElements(ArrayNode jsonNode, JsonTreeItem treeItem) {
        AtomicInteger i = new AtomicInteger(-1);
        jsonNode.elements().forEachRemaining(childNode -> {
            JsonTreeItem newItem = new JsonTreeItem(i.incrementAndGet() + "", null);
            treeItem.getChildren().add(newItem);
            treeItem.setExpanded(true);
            applyCorrectAdder(childNode, newItem);
        });
    }

    private static void recursivelyAddElements(TextNode val, JsonTreeItem treeItem) {
        treeItem.setPropertyName(treeItem.getValue());
        treeItem.setPropertyValue(val.textValue());
    }


    private static void recursivelyAddElements(String val, JsonTreeItem treeItem) {
        treeItem.setPropertyName(treeItem.getValue());
        treeItem.setPropertyValue(val);
    }

    private static void applyCorrectAdder(JsonNode value, JsonTreeItem treeItem) {
        if (value instanceof ObjectNode) {
            recursivelyAddElements(value, treeItem);
        } else if (value instanceof ArrayNode) {
            recursivelyAddElements((ArrayNode) value, treeItem);
        } else if (value instanceof TextNode) {
            recursivelyAddElements((TextNode) value, treeItem);
        } else {
            recursivelyAddElements((String.valueOf(value)), treeItem);
        }
    }

    public static Message fromJson(String json) {
        Struct.Builder structBuilder = Struct.newBuilder();
        try {
            JsonFormat.parser().ignoringUnknownFields().merge(json, structBuilder);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        return structBuilder.build();
    }

    public static String toJson(MessageOrBuilder messageOrBuilder) throws IOException {
        if (messageOrBuilder == null) {
            return null;
        }
        return JsonFormat.printer().print(messageOrBuilder);
    }
}
