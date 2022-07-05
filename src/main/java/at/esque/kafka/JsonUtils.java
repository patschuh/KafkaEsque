package at.esque.kafka;

import at.esque.kafka.controls.JsonTreeItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class JsonUtils {

    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

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
            path.setPropertyValue((path.getPropertyValue() == null ? "" : path.getPropertyValue()) + " -> " + ((jsonNode.get("value") instanceof TextNode)?jsonNode.get("value").textValue():jsonNode.get("value")));
            applyChangeTypeAndPropagateToChilds(path, "remove");
        } else {
            path.setPropertyValue(path.getPropertyValue() + " -> " + ((jsonNode.get("value") instanceof TextNode)?jsonNode.get("value").textValue():jsonNode.get("value")));
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
}
