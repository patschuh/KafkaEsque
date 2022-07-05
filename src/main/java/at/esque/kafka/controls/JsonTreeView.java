package at.esque.kafka.controls;

import at.esque.kafka.JsonUtils;
import at.esque.kafka.SystemUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;

import java.io.IOException;

public class JsonTreeView extends TreeView<String> {

    private StringProperty jsonString = new SimpleStringProperty();

    public JsonTreeView() {
        jsonString.addListener((observable, oldValue, newValue) -> {
            TreeItem<String> root;
            try {
                if (newValue != null && !newValue.isEmpty() && isVisible()) {
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
        setOnKeyPressed(new JsonTreeViewKeyEventHandler());
    }

    public String getJsonString() {
        return jsonString.get();
    }

    public StringProperty jsonStringProperty() {
        return jsonString;
    }

    public void setJsonString(String jsonString) {
        this.jsonString.set(jsonString);
    }

    private static class JsonTreeViewKeyEventHandler implements EventHandler<KeyEvent> {

        @Override
        public void handle(final KeyEvent keyEvent) {
            if (keyEvent.getSource() instanceof JsonTreeView) {
                if (KeyCodeCombinations.COPY.getCombination().match(keyEvent)) {
                    SystemUtils.copyStringSelectionToClipboard(() -> {
                        TreeItem<String> selectedItem = ((JsonTreeView) keyEvent.getSource()).getSelectionModel().getSelectedItem();
                        if (selectedItem != null) {
                            return String.valueOf(((JsonTreeItem) selectedItem).getPropertyValue());
                        }
                        return null;
                    });
                }
            }
        }
    }

    public void expand() {
        final TreeItem<String> root = this.getRoot();
        expand(root);
    }

    private void expand(TreeItem<?> item) {
        if (item.getChildren() != null && !item.getChildren().isEmpty()) {
            item.getChildren().forEach(this::expand);
        }
        item.setExpanded(true);
    }

}
