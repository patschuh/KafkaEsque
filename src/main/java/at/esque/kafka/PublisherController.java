package at.esque.kafka;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.alerts.SuccessAlert;
import at.esque.kafka.cluster.ClusterConfig;
import at.esque.kafka.cluster.TopicMessageTypeConfig;
import at.esque.kafka.controls.KafkaEsqueCodeArea;
import at.esque.kafka.handlers.ConfigHandler;
import at.esque.kafka.handlers.ProducerHandler;
import at.esque.kafka.handlers.ProducerWrapper;
import at.esque.kafka.topics.KafkaMessage;
import com.google.inject.Inject;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;


public class PublisherController {

    @FXML
    private ComboBox<Integer> partitionCombobox;
    @FXML
    private ComboBox<String> keyTypeSelectCombobox;
    @FXML
    private ComboBox<String> valueTypeSelectCombobox;
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
    @Inject
    private ConfigHandler configHandler;

    private UUID producerId;
    private ProducerWrapper producerWrapper;
    private TopicMessageTypeConfig configForTopic;
    private String topic;
    private Stage controlledStage;

    public void setup(ClusterConfig clusterConfig, String topic, ObservableList<Integer> partitions, KafkaMessage kafkaMessage) throws IOException {
        this.topic = topic;
        configForTopic = configHandler.getConfigForTopic(clusterConfig.getIdentifier(), topic);
        producerId = producerHandler.registerProducer(clusterConfig, topic);
        producerWrapper = producerHandler.getProducer(producerId).orElse(null);
        configHandler.configureKafkaEsqueCodeArea(keyTextArea);
        configHandler.configureKafkaEsqueCodeArea(valueTextArea);
        setupControls(partitions, configForTopic, kafkaMessage);

    }

    private void setupControls(ObservableList<Integer> partitions, TopicMessageTypeConfig configForTopic, KafkaMessage kafkaMessage) {
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

        ObservableList<String> topicMessageTypes = null;
        if(MessageType.AVRO_TOPIC_RECORD_NAME_STRATEGY.equals(configForTopic.getKeyType())){
            keyTypeSelectCombobox.setVisible(true);
            try {
                topicMessageTypes = findTypesForTopic(topic, producerWrapper.getSchemaRegistryRestService());
            } catch (RestClientException | IOException e) {
                ErrorAlert.show(e, getWindow());
            }
            if(topicMessageTypes != null) {
                keyTypeSelectCombobox.setItems(topicMessageTypes);
                if(kafkaMessage != null && kafkaMessage.getKeyType() != null){
                    valueTypeSelectCombobox.getSelectionModel().select(kafkaMessage.getKeyType());
                }
            }
        }
        if(MessageType.AVRO_TOPIC_RECORD_NAME_STRATEGY.equals(configForTopic.getValueType())){
            valueTypeSelectCombobox.setVisible(true);
            if(topicMessageTypes == null) {
                try {
                    topicMessageTypes = findTypesForTopic(topic, producerWrapper.getSchemaRegistryRestService());
                } catch (RestClientException | IOException e) {
                    ErrorAlert.show(e, getWindow());
                }
            }
            if(topicMessageTypes != null) {
                valueTypeSelectCombobox.setItems(topicMessageTypes);
                if(kafkaMessage != null && kafkaMessage.getValueType() != null){
                    valueTypeSelectCombobox.getSelectionModel().select(kafkaMessage.getValueType());
                }
            }
        }
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

        String valueRecordType = valueTypeSelectCombobox.getSelectionModel().getSelectedItem();
        String keyRecordType = keyTypeSelectCombobox.getSelectionModel().getSelectedItem();
        try {
            if (validateMessage(keyText, valueText)) {
                RecordMetadata metadata = producerHandler.sendMessage(producerId, topic, selectedPartition, keyText, valueText, keyRecordType, valueRecordType, headerTableView.getItems());
                String successMessage = String.format("topic [%s] " + System.lineSeparator() + "partition [%s]" + System.lineSeparator() + "offset [%s]", metadata.topic(), metadata.partition(), metadata.offset());
                SuccessAlert.show("Message published", "Message was published successfully", successMessage, getWindow());
            }
        } catch (Exception e) {
            ErrorAlert.show(e, getWindow());
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
        ErrorAlert.show(new ValidationException(message), getWindow(), false);
    }

    private Window getWindow() {
        return valueTextArea.getScene().getWindow();
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

    private ObservableList<String> findTypesForTopic(String topicName, RestService schemaRegistryService) throws IOException, RestClientException {
        List<String> collect = schemaRegistryService.getAllSubjects().stream()
                .filter(Objects::nonNull)
                .filter(subject -> subject.startsWith(topicName))
                .map(subject -> subject.split("-")[1])
                .filter(recordName -> !(recordName.equals("key") || recordName.equals("value")))
                .collect(Collectors.toList());
        return FXCollections.observableList(collect);
    }
}
