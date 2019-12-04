package at.esque.kafka;

import at.esque.kafka.controls.JsonTreeItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

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
        Map<String, Object> json = objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {
        });
        if (json == null) {
            return new JsonTreeItem(null, null);
        }
        JsonTreeItem root = new JsonTreeItem("root", null);
        root.setExpanded(true);
        recursivelyAddElements(json, root);
        return root;
    }

    private static void recursivelyAddElements(Map<String, Object> jsonNode, JsonTreeItem treeItem) {
        Set<Map.Entry<String, Object>> entries = jsonNode.entrySet();

        if (!entries.isEmpty()) {
            for (Map.Entry<String, Object> entry : entries) {
                JsonTreeItem newItem = new JsonTreeItem(entry.getKey(), null);
                treeItem.getChildren().add(newItem);
                applyCorrectAdder(entry.getValue(), newItem);
            }
        }
    }

    private static void recursivelyAddElements(ArrayList values, JsonTreeItem treeItem) {
        int i = 1;
        for (Object val : values) {
            JsonTreeItem newItem = new JsonTreeItem(i + "", null);
            treeItem.getChildren().add(newItem);
            treeItem.setExpanded(true);
            applyCorrectAdder(val, newItem);
            i++;
        }
    }

    private static void recursivelyAddElements(String val, JsonTreeItem treeItem) {
        treeItem.setPropertyName(treeItem.getValue());
        treeItem.setPropertyValue(val);
    }

    @SuppressWarnings("unchecked")
    private static void applyCorrectAdder(Object value, JsonTreeItem treeItem) {
        if (value instanceof Map) {
            recursivelyAddElements((Map<String, Object>) value, treeItem);
        } else if (value instanceof ArrayList) {
            recursivelyAddElements((ArrayList) value, treeItem);
        } else {
            recursivelyAddElements((String.valueOf(value)), treeItem);
        }
    }
}
