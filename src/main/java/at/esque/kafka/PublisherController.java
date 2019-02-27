package at.esque.kafka;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.alerts.SuccessAlert;
import at.esque.kafka.cluster.ClusterConfig;
import at.esque.kafka.cluster.TopicMessageTypeConfig;
import at.esque.kafka.topics.KafkaMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.client.rest.entities.Schema;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.TextFieldTableCell;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class PublisherController {

    private KafkaProducer publisher;
    private TopicMessageTypeConfig topic;
    private ObjectMapper mapper = new ObjectMapper();
    private RestService schemaRegistryRestService;

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

    public void setup(ClusterConfig clusterConfig, Map<String, String> configs, TopicMessageTypeConfig topicMessageTypeConfig, ObservableList<Integer> partitions, KafkaMessage kafkaMessage) {
        if (clusterConfig.getSchemaRegistry() == null || clusterConfig.getSchemaRegistry().isEmpty()) {
            if (topicMessageTypeConfig.containsAvro()) {
                throw new RuntimeException("Schema Registry URL not configured!");
            }
        }
        topic = topicMessageTypeConfig;
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, clusterConfig.getBootStrapServers());
        props.setProperty(ProducerConfig.CLIENT_ID_CONFIG, "kafkaesque-" + UUID.randomUUID());
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, topicMessageTypeConfig.getKeyType().equals(MessageType.AVRO) ? "io.confluent.kafka.serializers.KafkaAvroSerializer" : "org.apache.kafka.common.serialization.StringSerializer");
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, topicMessageTypeConfig.getValueType().equals(MessageType.AVRO) ? "io.confluent.kafka.serializers.KafkaAvroSerializer" : "org.apache.kafka.common.serialization.StringSerializer");
        props.setProperty("auto.register.schemas", "false");
        if (clusterConfig.getSchemaRegistry() != null) {
            props.setProperty("schema.registry.url", clusterConfig.getSchemaRegistry());
            schemaRegistryRestService = new RestService(clusterConfig.getSchemaRegistry());
        }


        props.putAll(configs);

        setupControls(partitions, kafkaMessage);

        publisher = new KafkaProducer(props);
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
        Integer selectedpartition = partitionCombobox.getSelectionModel().getSelectedItem();
        ProducerRecord<String, String> record;
        String keyText = keyText();
        String valueText = valueText();
        try {
            Object keyValue = topic.getKeyType() == MessageType.AVRO ? createRecord(keyText, topic.getName(), true) : keyText;
            Object valueValue = topic.getValueType() == MessageType.AVRO ? createRecord(valueText, topic.getName(), false) : valueText;
            if (selectedpartition != null && selectedpartition > -1) {
                record = new ProducerRecord(topic.getName(), selectedpartition, keyValue, valueValue);
            } else {
                record = new ProducerRecord(topic.getName(), keyValue, valueValue);
            }
            headerTableView.getItems().forEach(header -> record.headers().add(header));

            if (validateMessage(keyText, valueText)) {
                publishRecord(record);
            }
        } catch (Exception e) {
            ErrorAlert.show(e);
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
        headerTableView.getItems().add(new RecordHeader("key", "value".getBytes(StandardCharsets.UTF_8)));
    }

    @FXML
    private void removeHeaderClick(ActionEvent e) {
        Header selected = headerTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            headerTableView.getItems().remove(selected);
        }
    }

    private GenericRecord createRecord(String json, String topic, boolean isKey) throws IOException, RestClientException {

        Schema schema = getSchemaFromRegistry(topic + (isKey ? "-key" : "-value"));

        org.apache.avro.Schema avroSchema = new org.apache.avro.Schema.Parser().parse(schema.getSchema());

        JsonDecoder jsonDecoder = DecoderFactory.get().jsonDecoder(avroSchema, json);
        DatumReader<GenericRecord> reader = new GenericDatumReader<>(avroSchema);

        return reader.read(null, jsonDecoder);
    }

    private Schema getSchemaFromRegistry(String subject) throws IOException, RestClientException {
        return schemaRegistryRestService.getLatestVersion(subject);
    }


}
