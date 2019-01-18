package at.esque.kafka;

import at.esque.kafka.alerts.ConfirmationAlert;
import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.alerts.SuccessAlert;
import at.esque.kafka.alerts.TopicTemplateAppliedAlert;
import at.esque.kafka.cluster.ClusterConfig;
import at.esque.kafka.cluster.ClusterConfigs;
import at.esque.kafka.cluster.KafkaesqueAdminClient;
import at.esque.kafka.controls.JsonTreeView;
import at.esque.kafka.dialogs.AddClusterDialog;
import at.esque.kafka.dialogs.DeleteClustersDialog;
import at.esque.kafka.dialogs.TopicTemplatePartitionAndReplicationInputDialog;
import at.esque.kafka.dialogs.TraceInputDialog;
import at.esque.kafka.topics.*;
import at.esque.kafka.topics.model.Topic;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.inject.Inject;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.utils.Utils;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class Controller {

    private static final Pattern REPLACER_PATTERN = Pattern.compile("\\$\\{(?<identifier>.[^:{}]+):(?<type>.[^:{}]+)}");

    private static final String CONFIG_DIRECTORY = System.getProperty("user.home") + "/.kafkaesque/%s";

    private KafkaesqueAdminClient adminClient;

    private KafkaProducer<String, String> topicProducer;

    private File clusterConfig;

    private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);

    //Guice
    @Inject
    private BackGroundTaskHolder backGroundTaskHolder;
    @Inject
    private ConsumerHandler consumerHandler;
    //FXML
    @FXML
    private TextArea keyTextArea;
    @FXML
    private TextArea valueTextArea;
    @FXML
    public JsonTreeView jsonTreeView;
    @FXML
    private TableView<Header> headerTableView;
    @FXML
    private TableColumn<Header, String> headerKeyColumn;
    @FXML
    private TableColumn<Header, String> headerValueColumn;

    private ObservableList<KafkaMessage> baseList = FXCollections.observableArrayList();
    private FilteredList<KafkaMessage> filteredMessages = new FilteredList<>(baseList, km -> true);
    private SortedList<KafkaMessage> sortedMessages = new SortedList<>(filteredMessages);
    @FXML
    private TableView<KafkaMessage> messageTable;
    @FXML
    private TableColumn<KafkaMessage, Long> messageOffsetColumn;
    @FXML
    private TableColumn<KafkaMessage, Integer> messagePartitionColumn;
    @FXML
    private TableColumn<KafkaMessage, String> messageKeyColumn;
    @FXML
    private TableColumn<KafkaMessage, String> messageValueColumn;
    @FXML
    private TableColumn<KafkaMessage, String> messageTimestampColumn;
    @FXML
    private ListView<String> topicListView;
    @FXML
    private ComboBox<ClusterConfig> clusterComboBox;
    @FXML
    private Button refreshTopicListButton;
    @FXML
    private MenuItem playMessageBookMenu;
    @FXML
    private ComboBox<FetchTypes> fetchModeCombobox;
    @FXML
    private Button publishMessageButton;
    @FXML
    private Button getMessagesButton;
    @FXML
    private TextField numberOfMessagesToGetField;
    @FXML
    private TextField specificOffsetTextField;
    @FXML
    private ToggleButton formatJsonToggle;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private Label backgroundTaskDescription;
    @FXML
    private Label taskProgressLabel;
    @FXML
    private TextField topicFilterTextField;
    @FXML
    private TextField messageSearchTextField;
    @FXML
    private Button interruptMessagePollingButton;

    private Stage controlledStage;
    private ObjectMapper objectMapper = new ObjectMapper();
    private YAMLMapper yamlMapper = new YAMLMapper();

    public void setup(Stage controlledStage) {
        this.controlledStage = controlledStage;
        setUpLoadingIndicator();

        messageTable.setRowFactory(
                tableView -> {
                    final TableRow<KafkaMessage> row = new TableRow<>();
                    final ContextMenu rowMenu = new ContextMenu();
                    MenuItem openinPublisher = new MenuItem("open in publisher");
                    openinPublisher.setGraphic(new FontIcon(FontAwesome.SHARE));
                    openinPublisher.setOnAction(event -> showPublishMessageDialog(row.getItem()));
                    MenuItem openAsTxt = new MenuItem("open in text editor");
                    openAsTxt.setGraphic(new FontIcon(FontAwesome.EDIT));
                    openAsTxt.setOnAction(event -> openInTextEditor(row.getItem(), "txt"));
                    MenuItem openAsJson = new MenuItem("open in json editor");
                    openAsJson.setGraphic(new FontIcon(FontAwesome.EDIT));
                    openAsJson.setOnAction(event -> openInTextEditor(row.getItem(), "json"));
                    rowMenu.getItems().addAll(openinPublisher, openAsTxt, openAsJson);

                    // only display context menu for non-null items:
                    row.contextMenuProperty().bind(
                            Bindings.when(Bindings.isNotNull(row.itemProperty()))
                                    .then(rowMenu)
                                    .otherwise((ContextMenu) null));
                    return row;
                });
        interruptMessagePollingButton.disableProperty().bind(backGroundTaskHolder.isInProgressProperty().not());
        messageOffsetColumn.setCellValueFactory(new PropertyValueFactory<>("offset"));
        messagePartitionColumn.setCellValueFactory(new PropertyValueFactory<>("partition"));
        messageTimestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        messageKeyColumn.setCellValueFactory(param -> {
            if (param.getValue() != null && param.getValue().getKey() != null) {
                return new SimpleStringProperty(param.getValue().getKey().replaceAll("\\r\\n|\\r|\\n", " "));
            } else {
                return null;
            }
        });
        messageValueColumn.setCellValueFactory(param -> {
            if (param.getValue() != null && param.getValue().getValue() != null) {
                return new SimpleStringProperty(param.getValue().getValue().replaceAll("\\r\\n|\\r|\\n", " "));
            } else {
                return null;
            }
        });

        headerKeyColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().key()));
        headerValueColumn.setCellValueFactory(param -> new SimpleStringProperty(new String(param.getValue().value())));

        fetchModeCombobox.setItems(FXCollections.observableArrayList(FetchTypes.values()));

        fetchModeCombobox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                specificOffsetTextField.setVisible(newValue == FetchTypes.SPECIFIC_OFFSET));

        clusterComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            adminClient = new KafkaesqueAdminClient(newValue.getBootStrapServers());
            refreshTopicList(newValue);
        });

        topicListView.setCellFactory(lv -> topicListCellFactory());

        messageSearchTextField.textProperty().addListener((observable, oldValue, newValue) -> filteredMessages.setPredicate(km -> (km.getKey() != null && StringUtils.containsIgnoreCase(km.getKey(), newValue) || (km.getValue() != null && StringUtils.containsIgnoreCase(km.getValue(), newValue)))));

        sortedMessages.comparatorProperty().bind(messageTable.comparatorProperty());
        messageTable.setItems(sortedMessages);

        messageTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> updateKeyValueTextArea(newValue, formatJsonToggle.isSelected()));

        formatJsonToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            KafkaMessage selectedItem = messageTable.getSelectionModel().getSelectedItem();
            updateKeyValueTextArea(selectedItem, newValue);
        });

        setupClusterCombobox();
        loadOrCreateConfigs();

        topicFilterTextField.textProperty().addListener(((observable, oldValue, newValue) ->
                ((FilteredList<String>) topicListView.getItems()).setPredicate(t -> StringUtils.containsIgnoreCase(t, newValue))
        ));
        jsonTreeView.jsonStringPropertyProperty().bind(valueTextArea.textProperty());
        jsonTreeView.visibleProperty().bind(formatJsonToggle.selectedProperty());
        bindDisableProperties();
    }

    private void openInTextEditor(KafkaMessage value, String suffix) {
        File temp;
        try {
            temp = File.createTempFile("kafkaEsque-export", "." + suffix);

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(temp))) {
                bw.write(value.getValue());
            }

            Desktop.getDesktop().open(temp);
        } catch (IOException e) {
            ErrorAlert.show(e);
        }


    }

    private void bindDisableProperties() {
        BooleanProperty backgroundTaskInProgressProperty = backGroundTaskHolder.isInProgressProperty();
        topicListView.disableProperty().bind(backgroundTaskInProgressProperty);
        getMessagesButton.disableProperty().bind(backgroundTaskInProgressProperty);
        publishMessageButton.disableProperty().bind(backgroundTaskInProgressProperty);
        clusterComboBox.disableProperty().bind(backgroundTaskInProgressProperty);
        playMessageBookMenu.disableProperty().bind(backgroundTaskInProgressProperty);
        refreshTopicListButton.disableProperty().bind(backgroundTaskInProgressProperty);
    }

    private void updateKeyValueTextArea(KafkaMessage selectedMessage, boolean formatJson) {
        if (selectedMessage == null) {
            return;
        }
        headerTableView.setItems(selectedMessage.getHeaders());
        if (formatJson) {
            keyTextArea.setText(JsonUtils.formatJson(selectedMessage.getKey()));
            valueTextArea.setText(JsonUtils.formatJson(selectedMessage.getValue()));
        } else {
            keyTextArea.setText(selectedMessage.getKey());
            valueTextArea.setText(selectedMessage.getValue());
        }
    }

    private void setupClusterCombobox() {
        ListCell<ClusterConfig> buttonCell = new ListCell<ClusterConfig>() {
            @Override
            protected void updateItem(ClusterConfig item, boolean isEmpty) {
                super.updateItem(item, isEmpty);
                setText(item == null ? "" : item.getIdentifier());
            }
        };
        clusterComboBox.setButtonCell(buttonCell);
    }


    private ListCell<String> topicListCellFactory() {
        ListCell<String> cell = new ListCell<>();

        ContextMenu contextMenu = new ContextMenu();


        MenuItem infoItem = new MenuItem();
        infoItem.textProperty().set("describe");
        infoItem.setGraphic(new FontIcon(FontAwesome.INFO));
        infoItem.setOnAction(event -> showDescribeTopicDialog(topicListView.getSelectionModel().getSelectedItem()));
        MenuItem traceKeyItem = new MenuItem();
        traceKeyItem.setGraphic(new FontIcon(FontAwesome.KEY));
        traceKeyItem.textProperty().set("trace key");
        traceKeyItem.setOnAction(event -> {
            try {
                TraceInputDialog.show(true)
                        .ifPresent(traceKeyInput -> {
                            backGroundTaskHolder.setBackGroundTaskDescription("tracing key: " + traceKeyInput.getSearch());
                            Integer partition = null;
                            if (traceKeyInput.isFastTrace()) {
                                partition = getPartitionForKey(topicListView.getSelectionModel().getSelectedItem(), traceKeyInput.getSearch());
                            }
                            trace((ConsumerRecord<String, String> cr) -> StringUtils.equals(cr.key(), traceKeyInput.getSearch()), partition, traceKeyInput.getEpoch());
                        });
            } catch (Exception e) {
                ErrorAlert.show(e);
            }
        });

        MenuItem traceInValueItem = new MenuItem();
        traceInValueItem.setGraphic(new FontIcon(FontAwesome.SEARCH));
        traceInValueItem.textProperty().set("trace in value");
        traceInValueItem.setOnAction(event -> {
            try {
                TraceInputDialog.show(false)
                        .ifPresent(traceInput -> {
                            backGroundTaskHolder.setBackGroundTaskDescription("tracing in Value: " + traceInput.getSearch());
                            Pattern pattern = Pattern.compile(traceInput.getSearch());
                            trace((ConsumerRecord<String, String> cr) -> {
                                Matcher matcher = pattern.matcher(cr.value());
                                return matcher.find();
                            }, null, traceInput.getEpoch());
                        });
            } catch (Exception e) {
                ErrorAlert.show(e);
            }
        });

        MenuItem deleteItem = new MenuItem();
        deleteItem.setGraphic(new FontIcon(FontAwesome.TRASH));
        deleteItem.textProperty().set("delete");
        deleteItem.setOnAction(event -> {
            if (ConfirmationAlert.show("Delete Topic", "Topic [" + cell.itemProperty().get() + "] will be marked for deleteion.", "Are you sure you want to delete this topic")) {
                try {
                    adminClient.deleteTopic(cell.itemProperty().get());
                    SuccessAlert.show("Delete Topic", null, "Topic [" + cell.itemProperty().get() + "] marked for deleteion.");
                } catch (Exception e) {
                    ErrorAlert.show(e);
                }
            }
        });
        contextMenu.getItems().addAll(infoItem, traceKeyItem, traceInValueItem, deleteItem);

        cell.textProperty().bind(cell.itemProperty());

        cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
            if (isNowEmpty) {
                cell.setContextMenu(null);
            } else {
                cell.setContextMenu(contextMenu);
            }
        });
        return cell;
    }

    private void refreshTopicList(ClusterConfig newValue) {
        Platform.runLater(() -> topicListView.getItems().clear());

        backGroundTaskHolder.setBackGroundTaskDescription("getting Topics...");
        runInDaemonThread(() -> setupProducerAndConsumerForCluster(newValue));
    }

    private void setupProducerAndConsumerForCluster(ClusterConfig clusterConfig) {
        StopWatch stopWatch = new StopWatch();
        try {
            stopWatch.start();
            LOGGER.info("Starting producer setup process");
            backGroundTaskHolder.setIsInProgress(true);
            if (topicProducer != null) {
                LOGGER.info("Closing producer");
                topicProducer.close();
            }
            ObservableList<String> topics = FXCollections.observableArrayList(adminClient.getTopics());
            FilteredList<String> filteredTopics = new FilteredList<>(topics.sorted(), t -> true);
            Platform.runLater(() -> topicListView.setItems(filteredTopics));
            Properties props = new Properties();
            props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, clusterConfig.getBootStrapServers());
            props.setProperty(ProducerConfig.CLIENT_ID_CONFIG, "kafkaesque-" + UUID.randomUUID());
            props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
            props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
            LOGGER.info("Creating new Producer with properties: [{}]", props);
            topicProducer = new KafkaProducer<>(props);

        } finally {
            stopWatch.stop();
            LOGGER.info("Finished producer setup process [{}]", stopWatch);
            backGroundTaskHolder.backgroundTaskStopped();
        }

    }

    private void loadOrCreateConfigs() {
        clusterConfig = new File(String.format(CONFIG_DIRECTORY, "clusters.json"));
        if (clusterConfig.exists()) {
            try {
                ClusterConfigs clusterConfigs = objectMapper.readValue(clusterConfig, ClusterConfigs.class);
                clusterComboBox.setItems(clusterConfigs.getClusterConfigs());
            } catch (IOException e) {
                ErrorAlert.show(e);
            }
        } else {
            ClusterConfigs clusterConfigs = new ClusterConfigs();
            try {
                clusterConfig.getParentFile().mkdirs();
                objectMapper.writeValue(clusterConfig, clusterConfigs);
                clusterComboBox.setItems(clusterConfigs.getClusterConfigs());
            } catch (IOException e) {
                ErrorAlert.show(e);
            }
        }
    }

    private void saveClusterConfig() {
        try {
            ClusterConfigs clusterConfigs = new ClusterConfigs();
            clusterConfigs.setClusterConfigs(clusterComboBox.getItems());
            objectMapper.writeValue(clusterConfig, clusterConfigs);
        } catch (IOException e) {
            ErrorAlert.show(e);
        }
    }

    @FXML
    public void refreshButtonClick(ActionEvent e) {
        ClusterConfig selecctedCluster = clusterComboBox.getSelectionModel().getSelectedItem();
        if (selecctedCluster != null) {
            refreshTopicList(selecctedCluster);
        }
    }

    @FXML
    public void createTopicButtonClick(ActionEvent e) {
        showCreateTopicDialog();
    }

    @FXML
    public void onPublishMessageClick(ActionEvent click) {
        showPublishMessageDialog();
    }

    @FXML
    public void getMessagesClick(ActionEvent event) {
        backGroundTaskHolder.setBackGroundTaskDescription("getting messages...");
        FetchTypes fetchMode = fetchModeCombobox.getSelectionModel().getSelectedItem();
        if (fetchMode == FetchTypes.OLDEST) {
            getOldestMessages();
        } else if (fetchMode == FetchTypes.NEWEST) {
            getNewestMessages();
        } else if (fetchMode == FetchTypes.SPECIFIC_OFFSET) {
            getMessagesFromSpecificOffset();
        } else if (fetchMode == FetchTypes.CONTINUOUS) {
            getMessagesContinuously();
        }
    }

    @FXML
    public void schemaRegistryClick(ActionEvent event) {
        try {
            ClusterConfig selectedConfig = clusterComboBox.getSelectionModel().getSelectedItem();
            if (StringUtils.isEmpty(selectedConfig.getSchemaRegistry())) {
                Optional<String> input = showInputDialog("http://localhost:8081", "Add schema-registry url", "this cluster config is missing a schema registry url please add it now", "schema-registry URL");
                if (!input.isPresent()) {
                    return;
                }
                input.ifPresent(url -> {
                    selectedConfig.setSchemaRegistry(url);
                    saveClusterConfig();
                });
            }

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/schemaRegistryBrowser.fxml"));
            Parent root1 = fxmlLoader.load();
            SchemaRegistryBrowserController controller = fxmlLoader.getController();
            controller.setup(selectedConfig.getSchemaRegistry());
            Stage stage = new Stage();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/kafkaesque.png")));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Browse Schema Registry");
            stage.setScene(Main.createStyledScene(root1, -1, -1));
            stage.show();
        } catch (Exception e) {
            ErrorAlert.show(e);
        }
    }

    private void getOldestMessages() {
        runInDaemonThread(() -> {
            UUID consumerId = consumerHandler.registerConsumer(clusterComboBox.getSelectionModel().getSelectedItem());
            try {
                Map<Integer, AtomicLong> messagesConsumed = new HashMap<>();
                backGroundTaskHolder.setIsInProgress(true);
                consumerHandler.subscribe(consumerId, topicListView.getSelectionModel().getSelectedItem());
                Map<TopicPartition, Long> minOffsets = consumerHandler.getMinOffsets(consumerId);
                Map<TopicPartition, Long> maxOffsets = consumerHandler.getMaxOffsets(consumerId);
                consumerHandler.seekToOffset(consumerId, -1);
                baseList.clear();
                Map<TopicPartition, Long> currentOffsets = new HashMap<>();
                consumerHandler.getConsumer(consumerId).ifPresent(topicConsumer -> {
                    while (!backGroundTaskHolder.getStopBackGroundTask() && !reachedMaxOffsetForAllPartitionsOrGotEnoughMessages(maxOffsets, minOffsets, currentOffsets, messagesConsumed, getNumberOfMessagesToConsume())) {
                        receiveMessages(messagesConsumed, currentOffsets, topicConsumer, getNumberOfMessagesToConsume());
                    }
                });
            } finally {
                consumerHandler.deregisterConsumer(consumerId);
                backGroundTaskHolder.backgroundTaskStopped();
            }

        });
    }

    private long getNumberOfMessagesToConsume() {
        return Long.parseLong(numberOfMessagesToGetField.getText());
    }

    private void receiveMessages(Map<Integer, AtomicLong> messagesConsumedPerPartition, Map<TopicPartition, Long> currentOffsets, KafkaConsumer<String, String> topicConsumer, long numberToConsume) {
        ConsumerRecords<String, String> records = topicConsumer.poll(Duration.ofSeconds(1));
        records.forEach(record -> {
            long numberConsumed = messagesConsumedPerPartition.computeIfAbsent(record.partition(), key -> new AtomicLong(0)).get();
            currentOffsets.put(new TopicPartition(record.topic(), record.partition()), record.offset());
            if (numberConsumed < numberToConsume) {
                convertAndAdd(record);
                messagesConsumedPerPartition.computeIfAbsent(record.partition(), key -> new AtomicLong(0)).incrementAndGet();
            }
        });
        Platform.runLater(() -> backGroundTaskHolder.setProgressMessage(String.format("Consumed %s messages", messagesConsumedPerPartition.values().stream().mapToLong(AtomicLong::get).sum())));
    }

    private void getNewestMessages() {
        runInDaemonThread(() -> {
            UUID consumerId = consumerHandler.registerConsumer(clusterComboBox.getSelectionModel().getSelectedItem());
            try {
                Map<Integer, AtomicLong> messagesConsumed = new HashMap<>();
                backGroundTaskHolder.setIsInProgress(true);
                consumerHandler.subscribe(consumerId, topicListView.getSelectionModel().getSelectedItem());
                Map<TopicPartition, Long> minOffsets = consumerHandler.getMinOffsets(consumerId);
                Map<TopicPartition, Long> maxOffsets = consumerHandler.getMaxOffsets(consumerId);
                maxOffsets.forEach((topicPartition, maxOffset) -> {
                    long numberToLookBack = getNumberOfMessagesToConsume();
                    consumerHandler.getConsumer(consumerId).ifPresent(topicConsumer -> {
                        if (maxOffset - numberToLookBack > 0) {
                            topicConsumer.seek(topicPartition, maxOffset - numberToLookBack);
                        } else {
                            topicConsumer.seekToBeginning(Collections.singletonList(topicPartition));
                        }
                    });

                });
                baseList.clear();
                Map<TopicPartition, Long> currentOffsets = new HashMap<>();
                consumerHandler.getConsumer(consumerId).ifPresent(topicConsumer -> {
                    while (!backGroundTaskHolder.getStopBackGroundTask() && !reachedMaxOffsetForAllPartitions(maxOffsets, minOffsets, currentOffsets)) {
                        receiveMessages(messagesConsumed, currentOffsets, topicConsumer, getNumberOfMessagesToConsume());
                    }
                });
            } finally {
                consumerHandler.deregisterConsumer(consumerId);
                backGroundTaskHolder.backgroundTaskStopped();
            }
        });
    }

    private void getMessagesContinuously() {
        UUID consumerId = consumerHandler.registerConsumer(clusterComboBox.getSelectionModel().getSelectedItem());
        runInDaemonThread(() -> {
            try {
                AtomicLong messagesConsumed = new AtomicLong(0);
                backGroundTaskHolder.setIsInProgress(true);
                consumerHandler.subscribe(consumerId, topicListView.getSelectionModel().getSelectedItem());
                consumerHandler.seekToOffset(consumerId, -2);
                baseList.clear();
                consumerHandler.getConsumer(consumerId).ifPresent(topicConsumer -> {
                    while (!backGroundTaskHolder.getStopBackGroundTask()) {
                        ConsumerRecords<String, String> records = topicConsumer.poll(Duration.ofSeconds(1));
                        records.forEach(cr -> {
                            messagesConsumed.incrementAndGet();
                            convertAndAdd(cr);
                        });
                        Platform.runLater(() -> backGroundTaskHolder.setProgressMessage(String.format("Consumed %s messages", messagesConsumed)));
                    }
                });
            } finally {
                consumerHandler.deregisterConsumer(consumerId);
                backGroundTaskHolder.backgroundTaskStopped();
            }
        });
    }

    private void trace(Predicate<ConsumerRecord<String, String>> predicate, Integer fasttracePartition, Long epoch) {
        runInDaemonThread(() -> {
            UUID consumerId = consumerHandler.registerConsumer(clusterComboBox.getSelectionModel().getSelectedItem());
            try {
                backGroundTaskHolder.setIsInProgress(true);
                if (fasttracePartition != null) {
                    consumerHandler.getConsumer(consumerId).ifPresent(topicConsumer -> topicConsumer.assign(Collections.singletonList(new TopicPartition(topicListView.getSelectionModel().getSelectedItem(), fasttracePartition))));
                } else {
                    consumerHandler.subscribe(consumerId, topicListView.getSelectionModel().getSelectedItem());
                }
                AtomicLong messagesConsumed = new AtomicLong(0);
                AtomicLong messagesFound = new AtomicLong(0);
                Map<TopicPartition, Long> minOffsets = consumerHandler.getMinOffsets(consumerId);
                Map<TopicPartition, Long> maxOffsets = consumerHandler.getMaxOffsets(consumerId);
                if (epoch != null) {
                    consumerHandler.seekToTime(consumerId, epoch);
                } else {
                    consumerHandler.seekToOffset(consumerId, -1);
                }
                baseList.clear();
                Map<TopicPartition, Long> currentOffsets = new HashMap<>();
                consumerHandler.getConsumer(consumerId).ifPresent(topicConsumer -> {
                    while (!backGroundTaskHolder.getStopBackGroundTask() && !reachedMaxOffsetForAllPartitions(maxOffsets, minOffsets, currentOffsets)) {
                        ConsumerRecords<String, String> records = topicConsumer.poll(Duration.ofSeconds(1));
                        records.forEach(cr -> {
                            messagesConsumed.incrementAndGet();
                            currentOffsets.put(new TopicPartition(cr.topic(), cr.partition()), cr.offset());
                            if (predicate.test(cr)) {
                                convertAndAdd(cr);
                                messagesFound.incrementAndGet();
                            }
                        });
                        Platform.runLater(() -> backGroundTaskHolder.setProgressMessage(String.format("Found %s in %s consumed Messages", messagesFound, messagesConsumed)));
                    }
                });
            } finally {
                consumerHandler.deregisterConsumer(consumerId);
                backGroundTaskHolder.backgroundTaskStopped();
            }
        });
    }

    private int getPartitionForKey(String topic, String key) {
        DescribeTopicWrapper topicDescription = adminClient.describeTopic(topic);
        int numberOfPartitions = topicDescription.getTopicDescription().partitions().size();
        try {
            return Utils.toPositive(Utils.murmur2(key.getBytes("UTF-8"))) % numberOfPartitions;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private void convertAndAdd(ConsumerRecord<String, String> cr) {
        KafkaMessage kafkaMessage = new KafkaMessage();
        kafkaMessage.setOffset(cr.offset());
        kafkaMessage.setPartition(cr.partition());
        kafkaMessage.setKey(cr.key());
        kafkaMessage.setValue(cr.value());
        kafkaMessage.setTimestamp(Instant.ofEpochMilli(cr.timestamp()).toString());
        kafkaMessage.setHeaders(FXCollections.observableArrayList(cr.headers().toArray()));
        Platform.runLater(() -> baseList.add(kafkaMessage));
    }

    private void getMessagesFromSpecificOffset() {
        runInDaemonThread(() -> {
            UUID consumerId = consumerHandler.registerConsumer(clusterComboBox.getSelectionModel().getSelectedItem());
            try {
                Map<Integer, AtomicLong> messagesConsumed = new HashMap<>();
                long specifiedOffset = Long.parseLong(specificOffsetTextField.getText());
                backGroundTaskHolder.setIsInProgress(true);
                consumerHandler.subscribe(consumerId, topicListView.getSelectionModel().getSelectedItem());
                Map<TopicPartition, Long> minOffsets = consumerHandler.getMinOffsets(consumerId);
                Map<TopicPartition, Long> maxOffsets = consumerHandler.getMaxOffsets(consumerId);
                consumerHandler.seekToOffset(consumerId, specifiedOffset);
                baseList.clear();
                Map<TopicPartition, Long> currentOffsets = new HashMap<>();
                consumerHandler.getConsumer(consumerId).ifPresent(topicConsumer -> {
                    while (!backGroundTaskHolder.getStopBackGroundTask() && !reachedMaxOffsetForAllPartitionsOrGotEnoughMessages(maxOffsets, minOffsets, currentOffsets, messagesConsumed, getNumberOfMessagesToConsume())) {
                        receiveMessages(messagesConsumed, currentOffsets, topicConsumer, getNumberOfMessagesToConsume());
                    }
                });
            } finally {
                consumerHandler.deregisterConsumer(consumerId);
                backGroundTaskHolder.backgroundTaskStopped();
            }
        });
    }

    private boolean reachedMaxOffsetForAllPartitions
            (Map<TopicPartition, Long> maxOffsets, Map<TopicPartition, Long> minOffsets, Map<TopicPartition, Long> currentOffsets) {
        return maxOffsets.entrySet().stream()
                .filter(maxOffset -> (maxOffset.getValue() > -1 && maxOffset.getValue() > minOffsets.get(maxOffset.getKey()) && (currentOffsets.get(maxOffset.getKey()) == null || (maxOffset.getValue() - 1 > currentOffsets.get(maxOffset.getKey())))))
                .collect(Collectors.toList()).size() == 0;

    }

    private boolean reachedMaxOffsetForAllPartitionsOrGotEnoughMessages
            (Map<TopicPartition, Long> maxOffsets, Map<TopicPartition, Long> minOffsets, Map<TopicPartition, Long> currentOffsets, Map<Integer, AtomicLong> messagesConsumedPerPartition, long numberOfMessagesToConsume) {
        return maxOffsets.entrySet().stream()
                .filter(maxOffset -> {
                    AtomicLong atomicLong = messagesConsumedPerPartition.get(maxOffset.getKey().partition());
                    boolean enoughMessagesConsumed = atomicLong != null && atomicLong.get() < numberOfMessagesToConsume;
                    boolean reachedMaxOffset = maxOffset.getValue() > -1 && maxOffset.getValue() > minOffsets.get(maxOffset.getKey()) && (currentOffsets.get(maxOffset.getKey()) == null || (maxOffset.getValue() - 1 > currentOffsets.get(maxOffset.getKey())));
                    return reachedMaxOffset || enoughMessagesConsumed;
                })
                .collect(Collectors.toList()).size() == 0;

    }

    @FXML
    public void addClusterConfigClick(ActionEvent event) {
        AddClusterDialog.show().ifPresent(cc -> {
            clusterComboBox.getItems().add(cc);
            ClusterConfigs clusterConfigs = new ClusterConfigs();
            clusterConfigs.setClusterConfigs(clusterComboBox.getItems());
            try {
                objectMapper.writeValue(clusterConfig, clusterConfigs);
            } catch (IOException e) {
                ErrorAlert.show(e);
            }
        });
    }

    @FXML
    public void deleteClusterConfigsClick(ActionEvent event) {
        ObservableList<ClusterConfig> clusterConfigs = clusterComboBox.getItems();
        DeleteClustersDialog.show(clusterConfigs)
                .ifPresent(deletedClusterConfigs -> {
                    StringBuilder builder = new StringBuilder();
                    deletedClusterConfigs.forEach(config -> builder.append(config.toString()).append(System.lineSeparator()));
                    if (ConfirmationAlert.show("Deleting cluster configs", "The following configs will be permanently deleted:", builder.toString())) {
                        clusterConfigs.removeAll(deletedClusterConfigs);
                        saveClusterConfig();
                    }
                });
    }

    @FXML
    public void exportCsvClick(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save messages as csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV File (*.csv)", "*.csv"));
        File selectedFile = fileChooser.showSaveDialog(controlledStage);
        if (selectedFile != null) {
            try (Writer writer = new FileWriter(selectedFile.getAbsolutePath())) {
                StatefulBeanToCsv<KafkaMessage> beanToCsv = new StatefulBeanToCsvBuilder<KafkaMessage>(writer).build();
                messageTable.getItems().forEach(message -> {
                    try {
                        beanToCsv.write(message);
                    } catch (Exception e) {
                        ErrorAlert.show(e);
                    }
                });
            } catch (Exception e) {
                ErrorAlert.show(e);
            }
        }
    }

    @FXML
    public void stopPolling(ActionEvent event) {
        backGroundTaskHolder.setStopBackGroundTask(true);
        backGroundTaskHolder.setBackGroundTaskDescription("stop polling");
    }

    private void showPublishMessageDialog(KafkaMessage kafkaMessage) {
        try {
            List<Integer> partitions = adminClient.getTopicPatitions(topicListView.getSelectionModel().getSelectedItem());
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/publishMessage.fxml"));
            Parent root1 = fxmlLoader.load();
            PublisherController controller = fxmlLoader.getController();
            controller.setup(topicProducer, topicListView.getSelectionModel().getSelectedItem(), FXCollections.observableArrayList(partitions), kafkaMessage);
            Stage stage = new Stage();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/kafkaesque.png")));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Publish Message");
            stage.setScene(Main.createStyledScene(root1, -1, -1));
            stage.show();
        } catch (Exception e) {
            ErrorAlert.show(e);
        }
    }

    private void showPublishMessageDialog() {
        showPublishMessageDialog(null);
    }

    private void showCreateTopicDialog() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/createTopic.fxml"));
            Parent root1 = fxmlLoader.load();
            CreateTopicController controller = fxmlLoader.getController();
            controller.setup(adminClient);
            Stage stage = new Stage();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/kafkaesque.png")));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Create Topic");
            stage.setScene(Main.createStyledScene(root1, -1, -1));
            stage.show();
        } catch (Exception e) {
            ErrorAlert.show(e);
        }
    }

    private void showDescribeTopicDialog(String topic) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/describeTopic.fxml"));
            Parent root1 = fxmlLoader.load();
            DescribeTopicController controller = fxmlLoader.getController();
            controller.setup(adminClient.describeTopic(topic));
            Stage stage = new Stage();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/kafkaesque.png")));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Topic Description");
            stage.setScene(Main.createStyledScene(root1, -1, -1));
            stage.show();
        } catch (Exception e) {
            ErrorAlert.show(e);
        }
    }

    private void setUpLoadingIndicator() {
        backgroundTaskDescription.textProperty().bind(backGroundTaskHolder.backGroundTaskDescriptionProperty());
        loadingIndicator.visibleProperty().bind(backGroundTaskHolder.isInProgressProperty());
        backgroundTaskDescription.visibleProperty().bind(backGroundTaskHolder.isInProgressProperty());
        taskProgressLabel.textProperty().bind(backGroundTaskHolder.progressMessageProperty());
    }

    private void runInDaemonThread(Runnable runnable) {
        backGroundTaskHolder.setProgressMessage(null);
        Thread daemonThread = new Thread(runnable);
        daemonThread.setDaemon(true);
        daemonThread.start();
    }

    private Optional<String> showInputDialog(String defaultValue, String title, String header, String requestedInputLabel) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(this.getClass().getResource("/icons/kafkaesque.png").toString()));

        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(requestedInputLabel);
        Main.applyStylesheet(dialog.getDialogPane().getScene());

        return dialog.showAndWait();
    }

    // Experimental Area

    @FXML
    public void applyTopicTemplatesClick(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Topic Templates File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("YAML File (*.yaml, *.yml)", "*.yaml", "*.yml"));
        File selectedFile = fileChooser.showOpenDialog(controlledStage);
        if (selectedFile != null) {
            Optional<Pair<Integer, Short>> input = TopicTemplatePartitionAndReplicationInputDialog.show();
            if (!input.isPresent()) {
                return;
            }
            Pair<Integer, Short> partitionReplicationPair = input.get();
            Integer defaultPartitions = partitionReplicationPair.getKey();
            Short defualtReplication = partitionReplicationPair.getValue();
            backGroundTaskHolder.setBackGroundTaskDescription("creating Topics..");
            List<Topic> topicsToCreate;
            try {
                topicsToCreate = yamlMapper.readValue(selectedFile, new TypeReference<List<Topic>>() {
                });
            } catch (Exception e) {
                ErrorAlert.show(e);
                return;
            }
            runInDaemonThread(() -> {
                try {
                    backGroundTaskHolder.setIsInProgress(true);
                    List<String> alreadyExistedTopics = new ArrayList<>();
                    List<String> createdTopics = new ArrayList<>();

                    topicsToCreate.forEach(topic -> {
                        String currentTopic = topic.getName();
                        try {
                            adminClient.createTopic(topic.getName(),
                                    topic.getPartitions() > 0 ? topic.getPartitions() : defaultPartitions,
                                    topic.getReplacationFactor() > 0 ? topic.getReplacationFactor() : defualtReplication,
                                    topic.getConfigs());
                            createdTopics.add(currentTopic);
                        } catch (Exception e) {
                            if (e.getCause() instanceof TopicExistsException) {
                                alreadyExistedTopics.add(currentTopic);
                            } else {
                                Platform.runLater(() -> ErrorAlert.show(e));
                            }
                        }
                        Platform.runLater(() -> backGroundTaskHolder.setProgressMessage("Created Topic " + createdTopics.size() + " of " + topicsToCreate.size() + " (" + alreadyExistedTopics.size() + " already existed)"));
                    });
                    Platform.runLater(() -> TopicTemplateAppliedAlert.show(createdTopics, alreadyExistedTopics));
                } finally {
                    backGroundTaskHolder.backgroundTaskStopped();
                }
            });
        }
    }

    @FXML
    public void playMessageBook(ActionEvent event) {

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Message Book Folder");
        File selectedFolder = directoryChooser.showDialog(controlledStage);
        if (selectedFolder != null && ConfirmationAlert.show("Using experimental Feature", "This feature is experimental!!", "Are you sure you want to proceed? For all I know using this feature may break EVERYTHING.")) {
            runInDaemonThread(() -> {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                try {
                    backGroundTaskHolder.setIsInProgress(true);
                    List<File> listedFiles = Arrays.asList(Objects.requireNonNull(selectedFolder.listFiles()));
                    Map<String, String> replacementMap = new HashMap<>();
                    List<KafkaMessagBookWrapper> messagesToSend = new ArrayList<>();
                    Platform.runLater(() -> backGroundTaskHolder.setBackGroundTaskDescription("Playing Message Book: scanning messages"));
                    listedFiles.forEach(file -> {
                        if (!topicListView.getItems().contains(file.getName())) {
                            throw new RuntimeException(String.format("No such topic [%s] in current cluster", file.getName()));
                        }
                        addMessagesToSend(messagesToSend, file);
                    });
                    Platform.runLater(() -> backGroundTaskHolder.setBackGroundTaskDescription("Playing Message Book: building replacement map"));
                    messagesToSend.forEach(messageToSend -> addReplacementEntries(replacementMap, messageToSend));
                    Platform.runLater(() -> backGroundTaskHolder.setBackGroundTaskDescription("Playing Message Book: applying replacements"));
                    applyReplacements(messagesToSend, replacementMap);
                    Platform.runLater(() -> backGroundTaskHolder.setBackGroundTaskDescription("Playing Message Book: producing messages"));
                    AtomicInteger counter = new AtomicInteger(0);
                    messagesToSend.stream().sorted(Comparator.comparing(KafkaMessagBookWrapper::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())))
                            .forEach(message -> {
                                Future<RecordMetadata> future = topicProducer.send(new ProducerRecord<>(message.getTargetTopic(), message.getPartition() == -1 ? null : message.getPartition(), message.getKey(), message.getValue()));
                                try {
                                    future.get();
                                    Platform.runLater(() -> backGroundTaskHolder.setProgressMessage("published " + counter.incrementAndGet() + " messages"));
                                } catch (InterruptedException | ExecutionException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                } catch (Exception e) {
                    Platform.runLater(() -> ErrorAlert.show(e));
                } finally {
                    stopWatch.stop();
                    LOGGER.info("Message Book completed [{}]", stopWatch);
                    backGroundTaskHolder.backgroundTaskStopped();
                }
            });
        }
    }

    private void applyReplacements(List<KafkaMessagBookWrapper> messagesToSend, Map<String, String> replacementMap) {
        messagesToSend.forEach(messageToSend -> replacementMap.forEach((key, value) -> {
            messageToSend.setKey(messageToSend.getKey().replace(key, value));
            messageToSend.setValue(messageToSend.getValue().replace(key, value));
        }));
    }

    private void addMessagesToSend(List<KafkaMessagBookWrapper> messagesToSend, File playFile) {
        try {
            List<KafkaMessage> messages = new CsvToBeanBuilder<KafkaMessage>(new FileReader(playFile.getAbsolutePath()))
                    .withType(KafkaMessage.class).build().parse();
            messagesToSend.addAll(messages.stream().map(message -> new KafkaMessagBookWrapper(playFile.getName(), message))
                    .collect(Collectors.toList()));
        } catch (FileNotFoundException e) {
            Platform.runLater(() -> ErrorAlert.show(e));
        }
    }

    private void addReplacementEntries(Map<String, String> replacementMap, KafkaMessagBookWrapper message) {
        addReplacementEntries(replacementMap, message.getKey());
        addReplacementEntries(replacementMap, message.getValue());
    }

    private void addReplacementEntries(Map<String, String> replacementMap, String matchingString) {
        String replaceMentKeyFormat = "${%s:%s}";
        Matcher matcher = REPLACER_PATTERN.matcher(matchingString);

        while (matcher.find()) {
            String identifier = matcher.group("identifier");
            String type = matcher.group("type");

            Serializable replacement;
            switch (type) {
                case "UUID":
                    replacement = UUID.randomUUID();
                    break;
                default:
                    throw new RuntimeException("Unsupported replacement type: " + type);
            }

            replacementMap.put(String.format(replaceMentKeyFormat, identifier, type), replacement.toString());


        }

    }
}
