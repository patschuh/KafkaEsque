package at.esque.kafka;

import at.esque.kafka.controls.JsonTreeDiffView;
import at.esque.kafka.topics.KafkaMessage;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MessageDiffView {

    private final ObjectProperty<KafkaMessage> sourceKafkaMessage = new SimpleObjectProperty<>();
    private final ObjectProperty<KafkaMessage> targetKafkaMessage = new SimpleObjectProperty<>();

    @FXML
    public JsonTreeDiffView jsonDiffView;
    @FXML
    public Label sourcePartition;
    @FXML
    public Label targetPartition;
    @FXML
    public Label sourceOffset;
    @FXML
    public Label targetOffset;
    @FXML
    public Label sourceKey;
    @FXML
    public Label targetKey;

    @FXML
    public void initialize() {
        jsonDiffView.leftJsonStringProperty().bind(Bindings.createStringBinding(() -> sourceKafkaMessage.get() != null ? sourceKafkaMessage.get().getValue() : null, sourceKafkaMessage));
        jsonDiffView.rightJsonStringProperty().bind(Bindings.createStringBinding(() -> targetKafkaMessage.get() != null ? targetKafkaMessage.get().getValue() : null, targetKafkaMessage));

        sourcePartition.textProperty().bind(Bindings.createStringBinding(() -> sourceKafkaMessage.get() != null ? sourceKafkaMessage.get().getPartition() + "" : null, sourceKafkaMessage));
        sourceOffset.textProperty().bind(Bindings.createStringBinding(() -> sourceKafkaMessage.get() != null ? sourceKafkaMessage.get().getOffset() + "" : null, sourceKafkaMessage));
        sourceKey.textProperty().bind(Bindings.createStringBinding(() -> sourceKafkaMessage.get() != null ? sourceKafkaMessage.get().getKey() : null, sourceKafkaMessage));

        targetPartition.textProperty().bind(Bindings.createStringBinding(() -> targetKafkaMessage.get() != null ? targetKafkaMessage.get().getPartition() + "" : null, targetKafkaMessage));
        targetOffset.textProperty().bind(Bindings.createStringBinding(() -> targetKafkaMessage.get() != null ? targetKafkaMessage.get().getOffset() + "" : null, targetKafkaMessage));
        targetKey.textProperty().bind(Bindings.createStringBinding(() -> targetKafkaMessage.get() != null ? targetKafkaMessage.get().getKey() : null, targetKafkaMessage));
    }

    public void switchSourceAndTarget(ActionEvent event) {
        final KafkaMessage kafkaMessage = sourceKafkaMessage.get();
        sourceKafkaMessage.set(targetKafkaMessage.get());
        targetKafkaMessage.set(kafkaMessage);
    }

    public void setup(KafkaMessage source, KafkaMessage target) {
        sourceKafkaMessage.set(source);
        targetKafkaMessage.set(target);
    }
}
