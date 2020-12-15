package at.esque.kafka;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.cluster.ClusterConfig;
import at.esque.kafka.cluster.ClusterConfigs;
import at.esque.kafka.cluster.CrossClusterOperation;
import at.esque.kafka.cluster.KafkaesqueAdminClient;
import at.esque.kafka.cluster.TopicMessageTypeConfig;
import at.esque.kafka.controls.FilterableListView;
import at.esque.kafka.controls.InstantPicker;
import at.esque.kafka.exception.MissingSchemaRegistryException;
import at.esque.kafka.handlers.ConfigHandler;
import at.esque.kafka.handlers.ConsumerHandler;
import at.esque.kafka.handlers.CrossClusterOperationHandler;
import at.esque.kafka.handlers.ProducerHandler;
import com.google.inject.Inject;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrossClusterController {

    @FXML
    public InstantPicker instantPicker;
    @FXML
    public ToggleButton displayEpochToggle;
    @FXML
    private TextField specificKeyFIlterField;
    @FXML
    private TextField valueRegexFilterField;
    @FXML
    private TextField amountLimit;

    @FXML
    private FilterableListView<String> fromClusterTopicsList;
    @FXML
    private FilterableListView<String> toClusterTopicsList;
    @FXML
    private ListView<CrossClusterOperation> runningOperationsList;
    @FXML
    private ComboBox<ClusterConfig> fromClusterComboBox;
    @FXML
    private ComboBox<ClusterConfig> toClusterComboBox;

    private KafkaesqueAdminClient fromAdmin;
    private KafkaesqueAdminClient toAdmin;

    @Inject
    private CrossClusterOperationHandler crossClusterOperationHandler;
    @Inject
    private ConfigHandler configHandler;
    @Inject
    private ProducerHandler producerHandler;
    @Inject
    private ConsumerHandler consumerHandler;

    public void setup() {
        ClusterConfigs clusterConfigs = configHandler.loadOrCreateConfigs();

        instantPicker.setInstantValue(null);
        instantPicker.displayAsEpochProperty().bind(displayEpochToggle.selectedProperty());

        fromClusterTopicsList.setListComparator(String::compareTo);
        toClusterTopicsList.setListComparator(String::compareTo);

        fromClusterComboBox.setItems(clusterConfigs.getClusterConfigs());
        toClusterComboBox.setItems(clusterConfigs.getClusterConfigs());

        fromClusterComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            setupClusterControls(newValue, fromAdmin, fromClusterTopicsList);
        });

        toClusterComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            setupClusterControls(newValue, toAdmin, toClusterTopicsList);
        });

        runningOperationsList.setCellFactory(ropl -> {
            ListCell<CrossClusterOperation> cell = new ListCell<>();
            cell.itemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null) {
                    cell.textProperty().set("removed");
                } else {
                    cell.textProperty().set(MessageFormat.format("{0} / {1} ---> {2} / {3}", newValue.getFromCluster().getIdentifier(), newValue.getFromTopic().getName(), newValue.getToCluster().getIdentifier(), newValue.getToTopic().getName()));
                    cell.getItem().statusProperty().addListener((observable1, oldValue1, newValue1) -> {
                    });
                    cell.graphicProperty().bind(Bindings.createObjectBinding(() -> {
                        if (cell.getItem() != null) {
                            cell.setTooltip(new Tooltip(cell.getItem().getStatus()));
                            switch (cell.getItem().getStatus()) {
                                case "Created":
                                    return FontIcon.of(FontAwesome.INFO);
                                case "Running":
                                    return FontIcon.of(FontAwesome.PLAY_CIRCLE);
                                case "Finished":
                                    return FontIcon.of(FontAwesome.THUMBS_UP);
                                case "Stopped":
                                    return FontIcon.of(FontAwesome.STOP_CIRCLE);
                                default:
                                    if(cell.getItem().finishedExceptionaly()) {
                                        cell.setOnMouseClicked(mouseEvent -> ErrorAlert.show(cell.getItem().getException()));
                                    }
                                    return FontIcon.of(FontAwesome.WARNING);
                            }
                        }
                        return FontIcon.of(FontAwesome.WARNING);
                    }, cell.getItem().statusProperty()));
                }
            });

            return cell;
        });

        refreshOperationList(null);
    }

    private void setupClusterControls(ClusterConfig clusterConfig, KafkaesqueAdminClient adminClient, FilterableListView topicList) {
        if (adminClient != null) {
            adminClient.close();
        }
        adminClient = new KafkaesqueAdminClient(clusterConfig.getBootStrapServers(), configHandler.getSslProperties(clusterConfig),configHandler.getSaslProperties(clusterConfig));
        KafkaesqueAdminClient finalAdminClient = adminClient;
        runInDaemonThread(() -> {
            ObservableList<String> topics = FXCollections.observableArrayList(finalAdminClient.getTopics());
            Platform.runLater(() -> topicList.setItems(topics));
        });
    }

    private void startOperation(UUID operationId) {
        runInDaemonThread(() -> {
            CrossClusterOperation operation = crossClusterOperationHandler.getOperation(operationId);
            UUID producerId;
            UUID consumerId;
            try {
                producerId = producerHandler.registerProducer(operation.getToCluster());
                consumerId = consumerHandler.registerConsumer(operation.getFromCluster(), operation.getFromTopic(), configHandler.readConsumerConfigs(operation.getToCluster().getIdentifier()));
                consumerHandler.subscribe(consumerId, operation.getFromTopic().getName());
                if (instantPicker.getInstantValue() != null) {
                    consumerHandler.seekToTime(consumerId, instantPicker.getInstantValue().toEpochMilli());
                } else {
                    consumerHandler.seekToOffset(consumerId, -2);
                }
            } catch (IOException | MissingSchemaRegistryException e) {
                ErrorAlert.show(e);
                return;
            }
            Optional<KafkaConsumer> consumer = consumerHandler.getConsumer(consumerId);
            Platform.runLater(() -> operation.statusProperty().set("Running"));
            consumer.ifPresent(kafkaConsumer -> {
                AtomicLong count = new AtomicLong(0L);
                Long limit = StringUtils.isEmpty(amountLimit.getText()) ? null : Long.parseLong(amountLimit.getText());
                while (!operation.getStop().get() && (limit == null || count.get() < limit) && !operation.getStatus().equals("Error")) {
                    ConsumerRecords consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(5));
                    Iterable<ConsumerRecord> records = consumerRecords.records(operation.getFromTopic().getName());
                    records.forEach(consumerRecord -> {
                        try {
                            if (operation.getFilterFunction().test(consumerRecord)) {
                                ProducerRecord producerRecord = new ProducerRecord(operation.getToTopic().getName(), consumerRecord.key(), consumerRecord.value());
                                consumerRecord.headers().forEach(header -> producerRecord.headers().add(header));
                                producerHandler.sendRecord(producerId, producerRecord);
                                count.incrementAndGet();
                            }
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                operation.setException(e);
                                operation.setStatus("Error");
                            });
                        }
                    });
                }
                if (operation.getStop().get()) {
                    Platform.runLater(() -> operation.setStatus("Stopped"));
                } else if (!operation.getStatus().equals("Error")) {
                    Platform.runLater(() -> operation.setStatus("Finished"));
                }
            });
            consumerHandler.deregisterConsumer(consumerId);
            producerHandler.deregisterProducer(producerId);
        });
    }


    private void runInDaemonThread(Runnable runnable) {
        Thread daemonThread = new Thread(runnable);
        daemonThread.setDaemon(true);
        daemonThread.start();
    }

    @FXML
    public void startOperationClick(ActionEvent actionEvent) {
        try {
            ClusterConfig fromCluster = fromClusterComboBox.getSelectionModel().getSelectedItem();
            ClusterConfig toCluster = toClusterComboBox.getSelectionModel().getSelectedItem();

            String fromTopicName = fromClusterTopicsList.getListView().getSelectionModel().getSelectedItem();
            String toTopicName = toClusterTopicsList.getListView().getSelectionModel().getSelectedItem();

            TopicMessageTypeConfig fromTopic = configHandler.getConfigForTopic(fromCluster.getIdentifier(), fromTopicName);
            TopicMessageTypeConfig toTopic = configHandler.getConfigForTopic(toCluster.getIdentifier(), toTopicName);

            String keyFilter = specificKeyFIlterField.getText();
            String valueFilter = valueRegexFilterField.getText();
            Pattern pattern = StringUtils.isNotEmpty(valueFilter) ? Pattern.compile(valueFilter) : null;


            Predicate<ConsumerRecord> predicate = consumerRecord -> {
                boolean result = true;
                if (StringUtils.isNotEmpty(keyFilter)) {
                    result = keyFilter.equals(consumerRecord.key() != null ? consumerRecord.key().toString() : null);
                }
                if (pattern != null) {
                    Matcher matcher = pattern.matcher(consumerRecord.value().toString());
                    result = result && matcher.find();
                }

                return result;
            };

            CrossClusterOperation crossClusterOperation = new CrossClusterOperation(fromCluster, toCluster, fromTopic, toTopic, predicate);
            UUID operationId = crossClusterOperationHandler.registerOperation(crossClusterOperation);
            refreshOperationList(null);
            startOperation(operationId);
        } catch (Exception e) {
            ErrorAlert.show(e);
        }
    }

    @FXML
    public void refreshOperationList(ActionEvent actionEvent) {
        runningOperationsList.setItems(crossClusterOperationHandler.getOperations());
    }

    @FXML
    public void stopSelectedOperation(ActionEvent actionEvent) {
        CrossClusterOperation selectedItem = runningOperationsList.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getOperationId() != null) {
            crossClusterOperationHandler.markOperationForStop(selectedItem.getOperationId());
        }
    }
}
