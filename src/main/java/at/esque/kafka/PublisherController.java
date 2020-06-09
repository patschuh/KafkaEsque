package at.esque.kafka;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.alerts.SuccessAlert;
import at.esque.kafka.cluster.ClusterConfig;
import at.esque.kafka.controls.KafkaEsqueCodeArea;
import at.esque.kafka.handlers.ProducerHandler;
import at.esque.kafka.topics.KafkaMessage;
import com.google.inject.Inject;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;


public class PublisherController {

    @FXML
    private ComboBox<Integer> partitionCombobox;
    @FXML
    private KafkaEsqueCodeArea keyTextArea;
    @FXML
    private KafkaEsqueCodeArea valueTextArea;
    @FXML
    private CheckBox nullMessageToggle;
    @FXML
    private CheckBox validateIsJsonKeyBox;
    @FXML
    private CheckBox validateIsJsonValueBox;
    @FXML
    private TableView<Header> headerTableView;
    @FXML
    private TableColumn<Header, String> headerKeyColumn;
    @FXML
    private TableColumn<Header, String> headerValueColumn;

    @Inject
    private ProducerHandler producerHandler;

    private UUID producerId;
    private String topic;

    public void setup(ClusterConfig clusterConfig, String topic, ObservableList<Integer> partitions, KafkaMessage kafkaMessage) throws IOException {
        setupControls(partitions, kafkaMessage);
        producerId = producerHandler.registerProducer(clusterConfig);
        this.topic = topic;
    }

    private void setupControls(ObservableList<Integer> partitions, KafkaMessage kafkaMessage) {
        partitions.add(-1);
        partitionCombobox.setItems(partitions);
        partitionCombobox.getSelectionModel().select(Integer.valueOf(-1));
        valueTextArea.disableProperty().bind(nullMessageToggle.selectedProperty());

        if (kafkaMessage != null) {
            keyTextArea.setText(kafkaMessage.getKey());
            valueTextArea.setText(kafkaMessage.getValue());
            headerTableView.setItems(kafkaMessage.getHeaders());

        }

        headerKeyColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        headerValueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        headerKeyColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().key()));
        headerValueColumn.setCellValueFactory(param -> new SimpleStringProperty(new String(param.getValue().value())));
        headerKeyColumn.setOnEditCommit(event -> {
            Header current = event.getTableView().getItems().get(event.getTablePosition().getRow());
            event.getTableView().getItems().set(event.getTablePosition().getRow(), new RecordHeader(event.getNewValue(), current.value()));
        });
        headerValueColumn.setOnEditCommit(event -> {
            Header current = event.getTableView().getItems().get(event.getTablePosition().getRow());
            event.getTableView().getItems().set(event.getTablePosition().getRow(), new RecordHeader(current.key(), event.getNewValue().getBytes(StandardCharsets.UTF_8)));
        });
    }


    private String keyText() {
        return keyTextArea.getText();
    }

    private String valueText() {
        return valueTextArea.isDisable() ? null : valueTextArea.getText();
    }

    public void publishClick(ActionEvent event) {
        Integer selectedPartition = partitionCombobox.getSelectionModel().getSelectedItem();
        String keyText = keyText();
        String valueText = valueText();
        try {
            if (validateMessage(keyText, valueText)) {
                RecordMetadata metadata = producerHandler.sendMessage(producerId, topic, selectedPartition, keyText, valueText, headerTableView.getItems());
                String successMessage = String.format("topic [%s] " + System.lineSeparator() + "partition [%s]" + System.lineSeparator() + "offset [%s]", metadata.topic(), metadata.partition(), metadata.offset());
                SuccessAlert.show("Message published", "Message was published successfully", successMessage);
            }
        } catch (Exception e) {
            ErrorAlert.show(e);
        }

    }

    private boolean validateMessage(String key, String value) {
        boolean result = true;
        if (validateIsJsonKeyBox.isSelected()) {
            ValidationResult validationResult = JsonUtils.validate(key);

            if (!validationResult.isValid()) {
                result = false;
                showError("key", validationResult);
            }
        }
        if (validateIsJsonValueBox.isSelected()) {
            ValidationResult validationResult = JsonUtils.validate(value);

            if (!validationResult.isValid()) {
                result = false;
                showError("value", validationResult);
            }
        }
        return result;
    }

    private void showError(String inputName, ValidationResult validationResult) {
        String message = String.format("%s is not a valid json String: %s ", inputName, validationResult.getValidationError());
        ErrorAlert.show(new ValidationException(message), false);
    }

    @FXML
    private void addHeaderClick(ActionEvent event) {
        headerTableView.getItems().add(new RecordHeader("key", "value".getBytes(StandardCharsets.UTF_8)));
    }

    @FXML
    private void removeHeaderClick(ActionEvent e) {
        Header selected = headerTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            headerTableView.getItems().remove(selected);
        }
    }


    public void cleanup() {
        if (producerId != null) {
            producerHandler.deregisterProducer(producerId);
        }
    }
}
