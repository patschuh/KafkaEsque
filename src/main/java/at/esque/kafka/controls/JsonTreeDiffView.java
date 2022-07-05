package at.esque.kafka.controls;

import at.esque.kafka.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.zjsonpatch.DiffFlags;
import com.flipkart.zjsonpatch.JsonDiff;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.EnumSet;

public class JsonTreeDiffView extends SplitPane {
    private static final Logger logger = LoggerFactory.getLogger(JsonTreeDiffView.class);
    EnumSet<DiffFlags> flags = DiffFlags.dontNormalizeOpIntoMoveAndCopy().clone();

    private ObjectProperty<JsonNode> patch = new SimpleObjectProperty<>();
    private final StringProperty rightJsonString = new SimpleStringProperty();

    @FXML
    PatchJsonTreeView patchedTreeView;
    @FXML
    JsonTreeView patchTreeView;

    public JsonTreeDiffView() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "/fxml/controls/jsonTreeDiffView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.setClassLoader(getClass().getClassLoader());
        try {
            fxmlLoader.load();
        } catch (
                IOException exception) {
            throw new RuntimeException(exception);
        }
        setup();
    }

    private void setup() {
        patchedTreeView.jsonStringProperty().addListener((observable, oldValue, newValue) -> updatePatch());
        rightJsonString.addListener((observable, oldValue, newValue) -> updatePatch());
        patchTreeView.jsonStringProperty().bind(Bindings.createStringBinding(() -> patch.get() != null ? patch.get().toPrettyString() : null, patch));
        patchedTreeView.patchProperty().bind(patch);
        patchTreeView.jsonStringProperty().addListener((observable, oldValue, newValue) -> patchTreeView.expand());
    }

    public String getLeftJsonString() {
        return patchedTreeView.getJsonString();
    }

    public StringProperty leftJsonStringProperty() {
        return patchedTreeView.jsonStringProperty();
    }

    public void setLeftJsonString(String leftJsonString) {
        this.leftJsonStringProperty().set(leftJsonString);
    }

    public String getRightJsonString() {
        return rightJsonString.get();
    }

    public StringProperty rightJsonStringProperty() {
        return rightJsonString;
    }

    public void setRightJsonString(String rightJsonString) {
        this.rightJsonString.set(rightJsonString);
    }

    private void updatePatch() {
        try {
            final JsonNode leftNode = JsonUtils.readStringAsNode(getLeftJsonString());
            final JsonNode rightNode = JsonUtils.readStringAsNode(getRightJsonString());
            patch.set(JsonDiff.asJson(leftNode, rightNode, flags));
        } catch (Exception e) {
            patch.set(null);
            e.printStackTrace();
        }
    }
}
