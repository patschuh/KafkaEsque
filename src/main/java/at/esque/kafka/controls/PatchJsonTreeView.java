package at.esque.kafka.controls;

import at.esque.kafka.JsonUtils;
import at.esque.kafka.SystemUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Objects;

public class PatchJsonTreeView extends TreeView<String> {

    private StringProperty jsonString = new SimpleStringProperty();
    private ObjectProperty<JsonNode> patch = new SimpleObjectProperty<>();

    public PatchJsonTreeView() {
        jsonString.addListener((observable, oldValue, newValue) -> updateTree(newValue, patch.getValue()));
        patch.addListener((observable, oldValue, newValue) -> updateTree(jsonString.getValue(), newValue));
        setCellFactory(param -> {
            return new JsonTreeCell();
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

    public JsonNode getPatch() {
        return patch.get();
    }

    public ObjectProperty<JsonNode> patchProperty() {
        return patch;
    }

    public void setPatch(JsonNode patch) {
        this.patch.set(patch);
    }

    private void updateTree(String jsonString, JsonNode patch) {
        JsonTreeItem root;
        try {
            if (jsonString != null && !jsonString.isEmpty() && isVisible()) {
                if (patch != null) {
                    root = JsonUtils.buildPatchTreeFromJson(jsonString, (ArrayNode) patch);
                } else {
                    root = JsonUtils.buildTreeFromJson(jsonString);
                }
            } else {
                root = null;
            }
        } catch (IOException e) {
            root = new JsonTreeItem("error", "[Formatting Error]");
        }
        expand(root);
        this.setRoot(root);
        this.maxWidthProperty().bind(Bindings.when(visibleProperty()).then(Region.USE_COMPUTED_SIZE).otherwise(0));
    }

    private void expand(TreeItem<String> root){
        if(root.getChildren() != null && !root.getChildren().isEmpty()){
            root.getChildren().forEach(this::expand);
        }
        root.setExpanded(true);
    }

    private static class JsonTreeViewKeyEventHandler implements EventHandler<KeyEvent> {

        @Override
        public void handle(final KeyEvent keyEvent) {
            if (keyEvent.getSource() instanceof PatchJsonTreeView) {
                if (KeyCodeCombinations.COPY.getCombination().match(keyEvent)) {
                    SystemUtils.copyStringSelectionToClipboard(() -> {
                        TreeItem<String> selectedItem = ((PatchJsonTreeView) keyEvent.getSource()).getSelectionModel().getSelectedItem();
                        if (selectedItem != null) {
                            return String.valueOf(((JsonTreeItem) selectedItem).getPropertyValue());
                        }
                        return null;
                    });
                }
            }
        }
    }

    static class JsonTreeCell extends TreeCell<String> {
        private HBox hbox;

        private WeakReference<TreeItem<String>> treeItemRef;

        private InvalidationListener treeItemGraphicListener = observable -> {
            updateDisplay(getItem(), isEmpty());
        };

        private InvalidationListener treeItemListener = new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                TreeItem<String> oldTreeItem = treeItemRef == null ? null : treeItemRef.get();
                if (oldTreeItem != null) {
                    oldTreeItem.graphicProperty().removeListener(weakTreeItemGraphicListener);
                }

                TreeItem<String> newTreeItem = getTreeItem();
                if (newTreeItem != null) {
                    newTreeItem.graphicProperty().addListener(weakTreeItemGraphicListener);
                    treeItemRef = new WeakReference<TreeItem<String>>(newTreeItem);
                }
            }
        };

        private WeakInvalidationListener weakTreeItemGraphicListener =
                new WeakInvalidationListener(treeItemGraphicListener);

        private WeakInvalidationListener weakTreeItemListener =
                new WeakInvalidationListener(treeItemListener);

        public JsonTreeCell() {
            treeItemProperty().addListener(weakTreeItemListener);
            treeItemProperty().addListener((observable, oldValue, newValue) -> {
                setStyle("");
                if (newValue instanceof JsonTreeItem) {
                    if (Objects.equals(((JsonTreeItem) newValue).getPropertyChangedType(), "add")) {
                        setStyle("-fx-background-color: #4fff75;");
                    }
                    if (Objects.equals(((JsonTreeItem) newValue).getPropertyChangedType(), "remove")) {
                        setStyle("-fx-background-color: #ff4f55;");
                    }
                    if (Objects.equals(((JsonTreeItem) newValue).getPropertyChangedType(), "replace")) {
                        setStyle("-fx-background-color: #ffc941;");
                    }
                }
            });

            if (getTreeItem() != null) {
                getTreeItem().graphicProperty().addListener(weakTreeItemGraphicListener);
            }
        }

        void updateDisplay(String item, boolean empty) {
            if (item == null || empty) {
                hbox = null;
                setText(null);
                setGraphic(null);
            } else {
                // update the graphic if one is set in the TreeItem
                TreeItem<String> treeItem = getTreeItem();
                if (treeItem != null && treeItem.getGraphic() != null) {
                    hbox = null;
                    setText(item.toString());
                    setGraphic(treeItem.getGraphic());
                } else {
                    hbox = null;
                    setText(item.toString());
                    setGraphic(null);
                }
            }
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            updateDisplay(item, empty);
        }
    }
}
