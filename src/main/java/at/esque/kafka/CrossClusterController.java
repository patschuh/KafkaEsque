package at.esque.kafka;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.cluster.ClusterConfig;
import at.esque.kafka.cluster.ClusterConfigs;
import at.esque.kafka.cluster.CrossClusterOperation;
import at.esque.kafka.cluster.KafkaesqueAdminClient;
import at.esque.kafka.cluster.TopicMessageTypeConfig;
import at.esque.kafka.handlers.ConfigHandler;
import at.esque.kafka.handlers.ConsumerHandler;
import at.esque.kafka.handlers.CrossClusterOperationHandler;
import at.esque.kafka.handlers.ProducerHandler;
import com.google.inject.Inject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public class CrossClusterController {
    @FXML
    private ListView<String> fromClusterTopicsList;
    @FXML
    private ListView<String> toClusterTopicsList;
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
        fromClusterComboBox.setItems(clusterConfigs.getClusterConfigs());
        toClusterComboBox.setItems(clusterConfigs.getClusterConfigs());

        fromClusterComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            setupClusterControls(newValue, fromAdmin, fromClusterTopicsList);
        });

        toClusterComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            setupClusterControls(newValue, toAdmin, toClusterTopicsList);
        });

        refreshOperationList(null);
    }

    private void setupClusterControls(ClusterConfig clusterConfig, KafkaesqueAdminClient adminClient, ListView topicList) {
        if (adminClient != null) {
            adminClient.close();
        }
        adminClient = new KafkaesqueAdminClient(clusterConfig.getBootStrapServers());
        KafkaesqueAdminClient finalAdminClient = adminClient;
        runInDaemonThread(() -> {
            ObservableList<String> topics = FXCollections.observableArrayList(finalAdminClient.getTopics());
            Platform.runLater(() -> topicList.setItems(topics.sorted()));
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
                consumerHandler.seekToOffset(consumerId, -2);
            } catch (IOException e) {
                ErrorAlert.show(e);
                return;
            }
            Optional<KafkaConsumer> consumer = consumerHandler.getConsumer(consumerId);
            consumer.ifPresent(kafkaConsumer -> {
                while (!operation.getStop().get()) {
                    ConsumerRecords consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(5));
                    Iterable<ConsumerRecord> records = consumerRecords.records(operation.getFromTopic().getName());
                    records.forEach(consumerRecord -> {
                        try {
                            if (operation.getFilterFunction().test(consumerRecord)) {
                                producerHandler.sendMessage(producerId, operation.getToTopic().getName(), -1, (String) consumerRecord.key(), (String) consumerRecord.value());
                            }
                        } catch (Exception e) {
                            ErrorAlert.show(e);
                            return;
                        }
                    });
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

            String fromTopicName = fromClusterTopicsList.getSelectionModel().getSelectedItem();
            String toTopicName = toClusterTopicsList.getSelectionModel().getSelectedItem();

            TopicMessageTypeConfig fromTopic = configHandler.getConfigForTopic(fromCluster.getIdentifier(), fromTopicName);
            TopicMessageTypeConfig toTopic = configHandler.getConfigForTopic(toCluster.getIdentifier(), toTopicName);

            CrossClusterOperation crossClusterOperation = new CrossClusterOperation(fromCluster, toCluster, fromTopic, toTopic, consumerRecord -> true);
            UUID operationId = crossClusterOperationHandler.registerOperation(crossClusterOperation);
            startOperation(operationId);
        } catch (Exception e) {
            ErrorAlert.show(e);
            return;
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
