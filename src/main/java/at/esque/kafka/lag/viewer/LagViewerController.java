package at.esque.kafka.lag.viewer;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class LagViewerController {

    private static final Logger logger = LoggerFactory.getLogger(LagViewerController.class);

    @FXML
    public FilterableListView<Lag> consumerGroupList;
    @FXML
    public Label titleLabel;
    @FXML
    public FilterableListView<Lag> topicOffsetList;
    @FXML
    public Label topicTitleLabel;
    @FXML
    public FilterableListView<Lag> partitionOffsetList;

    private KafkaesqueAdminClient adminClient;

    private Set<String> consumedTopicFilterParameters;

    private BooleanProperty refreshRunning = new SimpleBooleanProperty(false);

    @FXML
    public void initialize() {
        consumerGroupList.getListView().setCellFactory(laggingEntityListView -> topicListCellFactory());
        topicOffsetList.getListView().setCellFactory(laggingEntityListView -> topicListCellFactory());
        partitionOffsetList.getListView().setCellFactory(laggingEntityListView -> topicListCellFactory());
    }

    public void setup(KafkaesqueAdminClient adminClient) {
        setup(adminClient, null);
    }

    public void setup(KafkaesqueAdminClient adminClient, Set<String> consumedTopicFilterParameters) {
        this.adminClient = adminClient;
        this.consumedTopicFilterParameters = consumedTopicFilterParameters;
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
                topicOffsetList.setItems(Collections.emptyList());
            } else {
                topicOffsetList.setItems(newValue.getSubEntities());
            }
        });

        topicTitleLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            Lag selectedItem = topicOffsetList.getListView().getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                return selectedItem.getTitle();
            }
            return "";
        }, topicOffsetList.getListView().getSelectionModel().selectedItemProperty()));
        topicOffsetList.setStringifierFunction(Lag::getTitle);
        topicOffsetList.setListComparator(Comparator.comparing(Lag::getTitle));
        topicOffsetList.filterTextFieldDisableProperty().bind(refreshRunning);
        topicOffsetList.setListComparator(Comparator.comparing(Lag::getTitle));
        topicOffsetList.getListView().getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
            if (newValue == null) {
                partitionOffsetList.setItems(Collections.emptyList());
            } else {
                partitionOffsetList.setItems(newValue.getSubEntities());
            }
        });

        partitionOffsetList.setStringifierFunction(Lag::getTitle);
        partitionOffsetList.setListComparator(Comparator.comparing(lag -> {
            try {
                return Integer.parseInt(lag.getTitle());
            } catch (NumberFormatException e) {
                logger.warn("partion lag title " + lag.getTitle() +" could not be parsed as int", e);
                return Integer.MAX_VALUE;
            }
        }));
        partitionOffsetList.filterTextFieldDisableProperty().bind(refreshRunning);

        this.adminClient = adminClient;
        startRefreshList();
    }

    @FXML
    private void startRefreshList() {
        AtomicBoolean refreshConsumerGroupsInFxThreadDone = new AtomicBoolean(false);
        runInDaemonThread(() -> {
            Platform.runLater(() -> {
                refreshRunning.setValue(true);
                consumerGroupList.setItems(FXCollections.observableArrayList(adminClient.getConsumerGroupLags(consumedTopicFilterParameters)));
                refreshConsumerGroupsInFxThreadDone.set(true);
            });
            while (!refreshConsumerGroupsInFxThreadDone.get()) {
                try {
                    Thread.sleep(100);
                    logger.info("Waiting for Fx Thread to update consumerGroup List");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            Platform.runLater(() -> refreshRunning.setValue(false));
        });
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
