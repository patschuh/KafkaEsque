package at.esque.kafka;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.alerts.SuccessAlert;
import at.esque.kafka.topics.KafkaMessage;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class PublisherController {

    private KafkaProducer<String, String> publisher;
    private String topic;

    @FXML
    private ComboBox<Integer> partitionCombobox;
    @FXML
    private TextArea keyTextArea;
    @FXML
    private TextArea valueTextArea;
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

    public void setup(KafkaProducer<String, String> publisher, String topic, ObservableList<Integer> partitions, KafkaMessage kafkaMessage) {
        this.publisher = publisher;
        this.topic = topic;
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
            try {
                event.getTableView().getItems().set(event.getTablePosition().getRow(), new RecordHeader(current.key(), event.getNewValue().getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                ErrorAlert.show(e);
            }
        });
    }

    private String keyText() {
        return keyTextArea.getText();
    }

    private String valueText() {
        return valueTextArea.isDisable() ? null : valueTextArea.getText();
    }

    public void publishClick(ActionEvent event) {
        Integer selectedpartition = partitionCombobox.getSelectionModel().getSelectedItem();
        ProducerRecord<String, String> record;
        if (selectedpartition != null && selectedpartition > -1) {
            record = new ProducerRecord<>(topic, selectedpartition, keyText(), valueText());
        } else {
            record = new ProducerRecord<>(topic, keyText(), valueText());
        }
        headerTableView.getItems().forEach(header -> record.headers().add(header));

        if (validateMessage(keyText(), valueText())) {
            publishRecord(record);
        }

    }

    private void publishRecord(ProducerRecord<String, String> record) {
        Future<RecordMetadata> future = publisher.send(record);
        try {
            RecordMetadata metadata = future.get(1, TimeUnit.MINUTES);
            String successMessage = String.format("topic [%s] " + System.lineSeparator() + "partition [%s]" + System.lineSeparator() + "offset [%s]", metadata.topic(), metadata.partition(), metadata.offset());
            SuccessAlert.show("Message published", "Message was published successfully", successMessage);
        } catch (TimeoutException e) {
            ErrorAlert.show(new RuntimeException("Timeout while waitung for RecordMetadata", e));
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
        try {
            headerTableView.getItems().add(new RecordHeader("key", "value".getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            ErrorAlert.show(e);
        }
    }

    @FXML
    private void removeHeaderClick(ActionEvent e) {
        Header selected = headerTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            headerTableView.getItems().remove(selected);
        }
    }


}
