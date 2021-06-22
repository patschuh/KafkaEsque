package at.esque.kafka.lag.viewer;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.cluster.KafkaesqueAdminClient;
import at.esque.kafka.controls.FilterableListView;
import at.esque.kafka.controls.LagViewerCellContent;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class LagViewerController {

    @FXML
    public FilterableListView<Lag> consumerGroupList;
    @FXML
    public Label titleLabel;
    @FXML
    public FilterableListView<Lag> topicOffsetList;

    private KafkaesqueAdminClient adminClient;

    private KafkaConsumer kafkaConsumer;

    private BooleanProperty refreshRunning = new SimpleBooleanProperty(false);

    @FXML
    public void initialize() {
        consumerGroupList.getListView().setCellFactory(laggingEntityListView -> topicListCellFactory());
        topicOffsetList.getListView().setCellFactory(laggingEntityListView -> topicListCellFactory());
    }

    public void setup(KafkaesqueAdminClient adminClient, KafkaConsumer kafkaConsumer) {
        this.adminClient = adminClient;
        this.kafkaConsumer = kafkaConsumer;
        titleLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            Lag selectedItem = consumerGroupList.getListView().getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                return selectedItem.getTitle();
            }
            return "";
        }, consumerGroupList.getListView().getSelectionModel().selectedItemProperty()));
        consumerGroupList.refreshButtonDisableProperty().bind(refreshRunning);
        consumerGroupList.filterTextFieldDisableProperty().bind(refreshRunning);
        consumerGroupList.setStringifierFunction(Lag::getTitle);
        consumerGroupList.setListComparator(Comparator.comparing(Lag::getTitle));

        consumerGroupList.getListView().getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
            if (newValue == null) {
                topicOffsetList.setItems(Collections.EMPTY_LIST);
            } else {
                topicOffsetList.setItems(newValue.getSubEntities());
            }
        });

        topicOffsetList.setStringifierFunction(Lag::getTitle);
        topicOffsetList.setListComparator(Comparator.comparing(Lag::getTitle));

        this.adminClient = adminClient;
        startRefreshList();
    }

    @FXML
    private void startRefreshList() {
        AtomicBoolean refreshConsumerGroupsInFxThreadDone = new AtomicBoolean(false);
        runInDaemonThread(() -> {
            Platform.runLater(() -> {
                refreshRunning.setValue(true);
                consumerGroupList.setItems(FXCollections.observableArrayList(adminClient.getConsumerGroups()));
                refreshConsumerGroupsInFxThreadDone.set(true);
            });
            while (!refreshConsumerGroupsInFxThreadDone.get()){
                try {
                    Thread.sleep(100);
                    System.out.println("Waiting for Fx Thread to update consumerGroup List");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            consumerGroupList.getListView().getItems().forEach(lag -> {
                try {
                    KafkaFuture<Map<TopicPartition, OffsetAndMetadata>> mapKafkaFuture = adminClient.listConsumerGroupOffsets(lag.getTitle()).partitionsToOffsetAndMetadata();
                    mapKafkaFuture.thenApply(topicPartitionOffsetAndMetadataMap -> {
                        if (this.kafkaConsumer != null) {
                            Map<TopicPartition, Long> offsetMap = getEndOffsets(topicPartitionOffsetAndMetadataMap);
                            long endOffsets = offsetMap.values().stream().mapToLong(Long::longValue).sum();
                            long currentOffsets = topicPartitionOffsetAndMetadataMap.values().stream().mapToLong(OffsetAndMetadata::offset).sum();
                            Platform.runLater(() -> {
                                lag.setEndOffset(endOffsets);
                                lag.setCurrentOffset(currentOffsets);
                                Collection<Lag> subEntites = createSubEntites(lag, topicPartitionOffsetAndMetadataMap, offsetMap);
                                lag.getSubEntities().clear();
                                lag.getSubEntities().addAll(subEntites);
                            });
                            return endOffsets - currentOffsets;
                        } else {
                            return 0;
                        }
                    }).get();
                } catch (Exception e) {
                    Platform.runLater(() -> ErrorAlert.show(e));
                }
            });
            Platform.runLater(() -> refreshRunning.setValue(false));
        });
    }

    private Map<TopicPartition, Long> getEndOffsets(Map<TopicPartition, OffsetAndMetadata> topicPartitionOffsetAndMetadataMap) {
        Map<TopicPartition, Long> endOffsets = this.kafkaConsumer.endOffsets(topicPartitionOffsetAndMetadataMap.keySet());
        if (endOffsets.keySet().size() < topicPartitionOffsetAndMetadataMap.keySet().size()) {
            endOffsets = getEndOffsets(topicPartitionOffsetAndMetadataMap);
        }
        return endOffsets;
    }

    private Collection<Lag> createSubEntites(Lag parentEntity, Map<TopicPartition, OffsetAndMetadata> currentOffsetMap, Map<TopicPartition, Long> endOffsetMap) {
        Map<String, Lag> topicEndOffsetMap = new HashMap<>();
        if (currentOffsetMap != null && endOffsetMap != null && parentEntity != null) {
            currentOffsetMap.forEach((key, value) -> {
                Lag lag = topicEndOffsetMap.computeIfAbsent(key.topic(), s -> new Lag(s, 0, 0));
                lag.setCurrentOffset(lag.getCurrentOffset() + value.offset());
                Long endOffset = endOffsetMap.get(key);
                if (endOffset != null) {
                    lag.setEndOffset(lag.getEndOffset() + endOffset);
                }
            });
        }
        return topicEndOffsetMap.values();
    }

    public void stop() {
        kafkaConsumer = null;
    }

    private ListCell<Lag> topicListCellFactory() {
        return new CustomListCell();
    }

    private static class CustomListCell extends ListCell<Lag> {
        private LagViewerCellContent cellContent;

        public CustomListCell() {
            super();
            cellContent = new LagViewerCellContent();
        }

        @Override
        protected void updateItem(Lag item, boolean empty) {
            super.updateItem(item, empty);
            if (item != null && !empty) {
                cellContent.bindLaggingEntity(item);
                setGraphic(cellContent);
            } else {
                setGraphic(null);
            }
        }
    }

    private void runInDaemonThread(Runnable runnable) {
        Thread daemonThread = new Thread(runnable);
        daemonThread.setDaemon(true);
        daemonThread.start();
    }

}
