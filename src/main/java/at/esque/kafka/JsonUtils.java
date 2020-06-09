package at.esque.kafka;

import at.esque.kafka.controls.JsonTreeItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public final class JsonUtils {
    private JsonUtils() {
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
            return new ValidationResult(true);
        }

        try {
            objectMapper.readTree(jsonInString);
            return new ValidationResult(true);
        } catch (IOException e) {
            return new ValidationResult(false, e.getMessage());
        }
    }

    public static JsonTreeItem buildTreeFromJson(String value) throws IOException {
        JsonNode json = objectMapper.readTree(value);
        if (json == null) {
            return new JsonTreeItem(null, null);
        }
        JsonTreeItem root = new JsonTreeItem("root", null);
        root.setExpanded(true);
        if(json instanceof ArrayNode){
            recursivelyAddElements((ArrayNode) json, root);
        }else if(json instanceof TextNode){
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
        AtomicInteger i = new AtomicInteger(0);
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
        }else {
            recursivelyAddElements((String.valueOf(value)), treeItem);
        }
    }
}
