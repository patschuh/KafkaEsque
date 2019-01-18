package at.esque.kafka.controls;

import at.esque.kafka.JsonUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Region;

import java.io.IOException;

public class JsonTreeView extends TreeView<String> {

    private StringProperty jsonString = new SimpleStringProperty();

    public JsonTreeView() {
        jsonString.addListener((observable, oldValue, newValue) -> {
            TreeItem<String> root;
            try {
                if (newValue != null && isVisible()) {
                    root = JsonUtils.buildTreeFromJson(newValue);
                } else {
                    root = null;
                }
            } catch (IOException e) {
                root = new TreeItem<>("[Formatting Error]");
            }
            this.setRoot(root);
            this.maxWidthProperty().bind(Bindings.when(visibleProperty()).then(Region.USE_COMPUTED_SIZE).otherwise(0));
        });
    }

    public String getJsonStringProperty() {
        return jsonString.get();
    }

    public StringProperty jsonStringPropertyProperty() {
        return jsonString;
    }

    public void setJsonStringProperty(String jsonStringProperty) {
        this.jsonString.set(jsonStringProperty);
    }
}
