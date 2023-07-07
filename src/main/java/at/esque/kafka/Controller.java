package at.esque.kafka;

import at.esque.kafka.acl.viewer.AclViewerController;
import at.esque.kafka.alerts.ConfirmationAlert;
import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.alerts.SuccessAlert;
import at.esque.kafka.alerts.TopicTemplateAppliedAlert;
import at.esque.kafka.cluster.ClusterConfig;
import at.esque.kafka.cluster.KafkaesqueAdminClient;
import at.esque.kafka.cluster.TopicMessageTypeConfig;
import at.esque.kafka.controls.FilterableListView;
import at.esque.kafka.controls.InstantPicker;
import at.esque.kafka.controls.JsonTreeView;
import at.esque.kafka.controls.KafkaEsqueCodeArea;
import at.esque.kafka.controls.MessagesTabContent;
import at.esque.kafka.controls.PinTab;
import at.esque.kafka.dialogs.ClusterConfigDialog;
import at.esque.kafka.dialogs.DeleteClustersDialog;
import at.esque.kafka.dialogs.SettingsDialog;
import at.esque.kafka.dialogs.TopicMessageTypeConfigDialog;
import at.esque.kafka.dialogs.TopicTemplatePartitionAndReplicationInputDialog;
import at.esque.kafka.dialogs.TraceInputDialog;
import at.esque.kafka.exception.MissingSchemaRegistryException;
import at.esque.kafka.handlers.ConfigHandler;
import at.esque.kafka.handlers.ConsumerHandler;
import at.esque.kafka.handlers.ProducerHandler;
import at.esque.kafka.handlers.Settings;
import at.esque.kafka.handlers.VersionInfoHandler;
import at.esque.kafka.lag.viewer.LagViewerController;
import at.esque.kafka.topics.CreateTopicController;
import at.esque.kafka.topics.DescribeTopicController;
import at.esque.kafka.topics.DescribeTopicWrapper;
import at.esque.kafka.topics.KafkaMessagBookWrapper;
import at.esque.kafka.topics.KafkaMessage;
import at.esque.kafka.topics.metadata.MessageMetaData;
import at.esque.kafka.topics.metadata.NumericMetadata;
import at.esque.kafka.topics.metadata.StringMetadata;
import at.esque.kafka.topics.model.Topic;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.opencsv.bean.CsvToBeanBuilder;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.utils.Utils;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Controller {

    private static final Pattern REPLACER_PATTERN = Pattern.compile("\\$\\{(?<identifier>.[^:{}]+):(?<type>.[^:{}]+)}");
    public static final String ICONS_KAFKAESQUE_PNG_PATH = "/icons/kafkaesque.png";

    private KafkaesqueAdminClient adminClient;

    private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);

    Scope rootScope = Scope.newEmptyScope();

    // Use BuiltinFunctionLoader to load built-in functions from the classpath.

    //Guice
    @Inject
    private BackGroundTaskHolder backGroundTaskHolder;
    @Inject
    private ConsumerHandler consumerHandler;
    @Inject
    private ProducerHandler producerHandler;
    @Inject
    private ConfigHandler configHandler;
    @Inject
    private VersionInfoHandler versionInfoHandler;
    @Inject
    private Injector injector;

    private HostServices hostServices;

    //FXML
    @FXML
    private Tooltip helpIconToolTip;
    @FXML
    private KafkaEsqueCodeArea keyTextArea;
    @FXML
    private Tab valueTab;
    @FXML
    private SplitPane valueSplitPane;
    @FXML
    private KafkaEsqueCodeArea valueTextArea;
    @FXML
    public JsonTreeView jsonTreeView;
    @FXML
    private TableView<Header> headerTableView;
    @FXML
    private TableColumn<Header, String> headerKeyColumn;
    @FXML
    private TableColumn<Header, String> headerValueColumn;
    @FXML
    private TableView<MessageMetaData> metdataTableView;
    @FXML
    private TableColumn<MessageMetaData, String> metadataNameColumn;
    @FXML
    private TableColumn<MessageMetaData, String> metadataValueColumn;
    @FXML
    private FilterableListView<String> topicListView;
    @FXML
    private ComboBox<ClusterConfig> clusterComboBox;
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
    public HBox specificFetchTypeInputHBox;
    @FXML
    private TextField specificOffsetTextField;
    @FXML
    public ComboBox<Integer> partitionCombobox;
    @FXML
    private InstantPicker specificInstantPicker;
    @FXML
    private ToggleButton formatJsonToggle;
    @FXML
    private TextField jqQueryField;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private Label backgroundTaskDescription;
    @FXML
    private Label taskProgressLabel;
    @FXML
    private Button interruptMessagePollingButton;
    @FXML
    private Button editClusterButton;
    @FXML
    private MenuBar menuBar;
    @FXML
    private TabPane messageTabPane;

    private KafkaMessage selectedMessage;

    private double[] cachedValueTabDividerPositions = null;

    private Stage controlledStage;
    private YAMLMapper yamlMapper = new YAMLMapper();

    private String selectedTopic() {
        return topicListView.getListView().getSelectionModel().getSelectedItem();
    }


    private ClusterConfig selectedCluster() {
        return clusterComboBox.getSelectionModel().getSelectedItem();
    }

    public void setup(Stage controlledStage) {
        BuiltinFunctionLoader.getInstance().loadFunctions(Version.LATEST, rootScope);
        this.controlledStage = controlledStage;
        setUpLoadingIndicator();
        String useSystemMenuBar = configHandler.getSettingsProperties().get(Settings.USE_SYSTEM_MENU_BAR);
        if (Boolean.parseBoolean(useSystemMenuBar)) {
            menuBar.setUseSystemMenuBar(true);
        }
        interruptMessagePollingButton.disableProperty().bind(backGroundTaskHolder.isInProgressProperty().not());

        headerKeyColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().key()));
        headerValueColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().value() == null ? null : new String(param.getValue().value())));

        metadataNameColumn.setCellValueFactory(param -> param.getValue().nameProperty());
        metadataValueColumn.setCellValueFactory(param -> param.getValue().valueAsString());

        fetchModeCombobox.setItems(FXCollections.observableArrayList(FetchTypes.values()));
        fetchModeCombobox.getSelectionModel().select(FetchTypes.NEWEST);

        fetchModeCombobox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
        {
            specificFetchTypeInputHBox.getChildren().remove(specificOffsetTextField);
            specificOffsetTextField.setVisible(false);
            specificFetchTypeInputHBox.getChildren().remove(specificInstantPicker);
            specificInstantPicker.setVisible(false);

            if (newValue == FetchTypes.SPECIFIC_OFFSET) {
                specificFetchTypeInputHBox.getChildren().add(specificOffsetTextField);
                specificOffsetTextField.setVisible(true);
            }
            if (newValue == FetchTypes.STARTING_FROM_INSTANT) {
                specificFetchTypeInputHBox.getChildren().add(specificInstantPicker);
                specificInstantPicker.setVisible(true);
            }
        });

        clusterComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            adminClient = new KafkaesqueAdminClient(newValue.getBootStrapServers(), configHandler.getSslProperties(selectedCluster()), configHandler.getSaslProperties(selectedCluster()));
            refreshTopicList();
        });

        partitionCombobox.getItems().add(-1);
        partitionCombobox.getSelectionModel().select(Integer.valueOf(-1));

        topicListView.getListView().setCellFactory(lv -> topicListCellFactory());
        topicListView.setListComparator(String::compareTo);
        topicListView.getListView().getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                try {
                    final Integer selectedItem = partitionCombobox.getSelectionModel().getSelectedItem();
                    final List<Integer> topicPatitions = adminClient.getPatitions(newValue);
                    partitionCombobox.getItems().clear();
                    partitionCombobox.getItems().add(-1);
                    partitionCombobox.getItems().addAll(topicPatitions);
                    if (selectedItem != null && partitionCombobox.getItems().contains(selectedItem)) {
                        partitionCombobox.getSelectionModel().select(selectedItem);
                    } else {
                        partitionCombobox.getSelectionModel().select(-1);
                    }
                } catch (Exception e) {
                    LOGGER.warn("failed to get topic partitions", e);
                }
            }
        });

        headerTableView.setOnKeyPressed(generateHeaderTableEventHandler());
        metdataTableView.setOnKeyPressed(generateMetadataTableEventHandler());

        configHandler.configureKafkaEsqueCodeArea(keyTextArea);
        configHandler.configureKafkaEsqueCodeArea(valueTextArea);

        setupJsonFormatToggle();

        setupClusterCombobox();
        clusterComboBox.setItems(configHandler.loadOrCreateConfigs().getClusterConfigs());

        jsonTreeView.jsonStringProperty().bind(valueTextArea.textProperty());
        jsonTreeView.visibleProperty().bind(formatJsonToggle.selectedProperty());
        bindDisableProperties();
        ClusterConfig dummycluster = new ClusterConfig();
        dummycluster.setIdentifier("Empty");
        messageTabPane.getTabs().add(createTab(dummycluster, "Tab"));
        helpIconToolTip.setText(buildToolTip());

        versionInfoHandler.showDialogIfUpdateIsAvailable(hostServices);
    }

    private void setupJsonFormatToggle() {
        formatJsonToggle.selectedProperty().addListener((observable, oldValue, newValue) -> updateKeyValueTextArea(selectedMessage, newValue));
        jqQueryField.textProperty().addListener((observable, oldValue, newValue) -> updateKeyValueTextArea(selectedMessage, formatJsonToggle.isSelected()));
        jqQueryField.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            if (formatJsonToggle.isSelected()) {
                jqQueryField.setMaxWidth(-1);
                return true;
            }
            jqQueryField.setMaxWidth(0);
            return false;
        }, formatJsonToggle.selectedProperty()));
        updateValueTabLayout(formatJsonToggle.isSelected());
    }

    private void openInTextEditor(KafkaMessage value, String suffix) {
        if (Desktop.isDesktopSupported()) {
            new Thread(() -> {
                File temp;
                try {
                    temp = File.createTempFile("kafkaEsque-export", "." + suffix);

                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(temp))) {
                        bw.write(value.getValue());
                    }

                    Desktop.getDesktop().open(temp);
                } catch (IOException e) {
                    Platform.runLater(() -> ErrorAlert.show(e, controlledStage));
                }
            }).start();
        }


    }

    private void bindDisableProperties() {
        BooleanProperty backgroundTaskInProgressProperty = backGroundTaskHolder.isInProgressProperty();
        topicListView.disableProperty().bind(backgroundTaskInProgressProperty);
        getMessagesButton.disableProperty().bind(backgroundTaskInProgressProperty);
        publishMessageButton.disableProperty().bind(backgroundTaskInProgressProperty);
        clusterComboBox.disableProperty().bind(backgroundTaskInProgressProperty);
        playMessageBookMenu.disableProperty().bind(backgroundTaskInProgressProperty);
        editClusterButton.disableProperty().bind(clusterComboBox.getSelectionModel().selectedItemProperty().isNull());
    }

    private void updateKeyValueTextArea(KafkaMessage selectedMessage, boolean formatJson) {
        updateValueTabLayout(formatJson);
        if (selectedMessage == null) {
            return;
        }
        headerTableView.setItems(selectedMessage.getHeaders());
        metdataTableView.setItems(selectedMessage.getMetaData());

        if (formatJson) {
            keyTextArea.setText(JsonUtils.formatJson(selectedMessage.getKey()));
            String prettyPrintText = JsonUtils.formatJson(selectedMessage.getValue());

            try {
                JsonQuery jqQuery = JsonQuery.compile(StringUtils.isEmpty(jqQueryField.getText()) ? "." : jqQueryField.getText(), Version.LATEST);
                final List<JsonNode> out = new ArrayList<>();
                jqQuery.apply(Scope.newChildScope(rootScope), JsonUtils.readStringAsNode(selectedMessage.getValue()), out::add);
                if (out.size() == 1) {
                    prettyPrintText = JsonUtils.formatJson(out.get(0));
                } else {
                    prettyPrintText = JsonUtils.formatJson(out);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            valueTextArea.setText(prettyPrintText);
        } else {
            keyTextArea.setText(selectedMessage.getKey());
            valueTextArea.setText(selectedMessage.getValue());
        }
    }

    private void updateValueTabLayout(boolean formatJson) {
        if (formatJson) {
            if (!valueSplitPane.getItems().contains(valueTextArea)) {
                valueSplitPane.getItems().add(valueTextArea);
            }
            if (cachedValueTabDividerPositions != null && cachedValueTabDividerPositions.length > 0) {
                valueSplitPane.setDividerPositions(cachedValueTabDividerPositions);
            }
            valueTab.setContent(valueSplitPane);
        } else {
            cachedValueTabDividerPositions = valueSplitPane.getDividerPositions();
            valueSplitPane.getItems().remove(valueTextArea);
            valueTab.setContent(valueTextArea);
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
        infoItem.setOnAction(event -> showDescribeTopicDialog(selectedTopic()));

        MenuItem configMessageTypesItem = new MenuItem();
        configMessageTypesItem.textProperty().set("configure message types");
        configMessageTypesItem.setGraphic(new FontIcon(FontAwesome.WRENCH));
        configMessageTypesItem.setOnAction(event -> {
            TopicMessageTypeConfig config = configHandler.getConfigForTopic(selectedCluster().getIdentifier(), selectedTopic());
            TopicMessageTypeConfigDialog.show(config);
            configHandler.saveTopicMessageTypeConfigs(selectedCluster().getIdentifier());
        });

        MenuItem traceItem = new MenuItem();
        traceItem.setGraphic(new FontIcon(FontAwesome.SEARCH));
        traceItem.textProperty().set("trace message");
        traceItem.setOnAction(event -> {
            try {
                TopicMessageTypeConfig topicMessageTypeConfig = configHandler.getConfigForTopic(selectedCluster().getIdentifier(), selectedTopic());
                Map<String, String> consumerConfig = configHandler.readConsumerConfigs(selectedCluster().getIdentifier());
                TraceInputDialog.show(topicMessageTypeConfig.getKeyType() == MessageType.AVRO, Settings.isTraceQuickSelectEnabled(configHandler.getSettingsProperties()), Settings.readDurationSetting(configHandler.getSettingsProperties()), Integer.parseInt(configHandler.getSettingsProperties().get(Settings.RECENT_TRACE_MAX_ENTRIES)), partitionCombobox.getItems())
                        .ifPresent(traceInput -> {
                            backGroundTaskHolder.setBackGroundTaskDescription("tracing message");
                            Integer partition = null;
                            if (!traceInput.getConditionMode().equals("value only") && !traceInput.getConditionMode().equals("OR") && topicMessageTypeConfig.getKeyType() != MessageType.AVRO && traceInput.isFastTrace()) {
                                partition = getPartitionForKey(selectedTopic(), traceInput.getKeySearch());
                            } else if (!traceInput.isFastTrace() && traceInput.getPartition() != null && traceInput.getPartition() > -1) {
                                partition = traceInput.getPartition();
                            }
                            Predicate<ConsumerRecord> keyPredicate = null;
                            Predicate<ConsumerRecord> valuePredicate = null;
                            Predicate<ConsumerRecord> actualPredicate = null;


                            if (!traceInput.getConditionMode().equals("value only")) {
                                keyPredicate = TraceUtils.keyPredicate(traceInput.getKeySearch(), traceInput.getKeyMode());
                            }
                            if (!traceInput.getConditionMode().equals("key only")) {
                                valuePredicate = TraceUtils.valuePredicate(traceInput.getValueSearch(), traceInput.isSearchNull());
                            }

                            if (traceInput.getConditionMode().equals("key only")) {
                                actualPredicate = keyPredicate;
                            } else if (traceInput.getConditionMode().equals("value only")) {
                                actualPredicate = valuePredicate;
                            } else if (traceInput.getConditionMode().equals("AND")) {
                                actualPredicate = keyPredicate.and(valuePredicate);
                            } else if (traceInput.getConditionMode().equals("OR")) {
                                actualPredicate = keyPredicate.or(valuePredicate);
                            }

                            trace(topicMessageTypeConfig, consumerConfig, actualPredicate, partition, traceInput.getEpochStart(), traceInput.getEpochEnd() == null ? null : consumerRecord -> consumerRecord.timestamp() >= traceInput.getEpochEnd());
                        });
            } catch (Exception e) {
                ErrorAlert.show(e, controlledStage);
            }
        });

        MenuItem deleteItem = new MenuItem();
        deleteItem.setGraphic(new FontIcon(FontAwesome.TRASH));
        deleteItem.textProperty().set("delete");
        deleteItem.setOnAction(event -> {
            if (ConfirmationAlert.show("Delete Topic", "Topic [" + cell.itemProperty().get() + "] will be marked for deletion.", "Are you sure you want to delete this topic")) {
                try {
                    adminClient.deleteTopic(cell.itemProperty().get());
                    SuccessAlert.show("Delete Topic", null, "Topic [" + cell.itemProperty().get() + "] marked for deletion.");
                } catch (Exception e) {
                    ErrorAlert.show(e, controlledStage);
                }
            }
        });
        contextMenu.getItems().addAll(infoItem, configMessageTypesItem, traceItem, deleteItem);

        cell.textProperty().bind(cell.itemProperty());

        cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
            if (isNowEmpty) {
                cell.setContextMenu(null);
            } else {
                cell.setContextMenu(contextMenu);
            }
        });

        cell.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                this.getMessagesClick(new ActionEvent(event.getSource(), event.getTarget()));
            }
        });

        return cell;
    }

    private void refreshTopicList() {
        backGroundTaskHolder.setBackGroundTaskDescription("getting Topics...");
        runInDaemonThread(() -> getTopicsForCluster());
    }

    private void getTopicsForCluster() {
        StopWatch stopWatch = new StopWatch();
        try {
            stopWatch.start();
            LOGGER.info("Started getting topics for cluster");
            backGroundTaskHolder.setIsInProgress(true);
            Set<String> topics = adminClient.getTopics();
            Platform.runLater(() -> topicListView.setItems(topics));

        } finally {
            stopWatch.stop();
            LOGGER.info("Finished getting topics for cluster [{}]", stopWatch);
            backGroundTaskHolder.backgroundTaskStopped();
        }
    }

    @FXML
    public void refreshButtonClick(ActionEvent e) {
        refreshTopicList();
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
        try {
            TopicMessageTypeConfig topicMessageTypeConfig = configHandler.getConfigForTopic(selectedCluster().getIdentifier(), selectedTopic());
            Map<String, String> consumerConfig = configHandler.readConsumerConfigs(selectedCluster().getIdentifier());

            backGroundTaskHolder.setBackGroundTaskDescription("getting messages...");
            FetchTypes fetchMode = fetchModeCombobox.getSelectionModel().getSelectedItem();
            if (fetchMode == FetchTypes.OLDEST) {
                getOldestMessages(topicMessageTypeConfig, consumerConfig);
            } else if (fetchMode == FetchTypes.NEWEST) {
                getNewestMessages(topicMessageTypeConfig, consumerConfig);
            } else if (fetchMode == FetchTypes.SPECIFIC_OFFSET) {
                getMessagesFromSpecificOffset(topicMessageTypeConfig, consumerConfig);
            } else if (fetchMode == FetchTypes.CONTINUOUS) {
                getMessagesContinuously(topicMessageTypeConfig, consumerConfig);
            } else if (fetchMode == FetchTypes.STARTING_FROM_INSTANT) {
                getMessagesStartingFromInstant(topicMessageTypeConfig, consumerConfig);
            }
        } catch (IOException e) {
            ErrorAlert.show(e, controlledStage);
        }
    }

    @FXML
    public void schemaRegistryClick(ActionEvent event) {
        try {
            ClusterConfig selectedConfig = selectedCluster();
            if (StringUtils.isEmpty(selectedConfig.getSchemaRegistry())) {
                Optional<String> input = SystemUtils.showInputDialog("http://localhost:8081", "Add schema-registry url", "this cluster config is missing a schema registry url please add it now", "schema-registry URL");
                if (!input.isPresent()) {
                    return;
                }
                input.ifPresent(url -> {
                    selectedConfig.setSchemaRegistry(url);
                    configHandler.saveConfigs();
                });
            }

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/schemaRegistryBrowser.fxml"));
            Parent root1 = fxmlLoader.load();
            SchemaRegistryBrowserController controller = fxmlLoader.getController();
            controller.setup(selectedConfig, configHandler);
            Stage stage = new Stage();
            stage.getIcons().add(new Image(getClass().getResourceAsStream(ICONS_KAFKAESQUE_PNG_PATH)));
            stage.initOwner(controlledStage);
            stage.initModality(Modality.NONE);
            stage.setTitle("Browse Schema Registry - " + selectedConfig.getIdentifier());
            stage.setScene(Main.createStyledScene(root1, -1, -1));
            stage.show();
            centerStageOnControlledStage(stage);
        } catch (Exception e) {
            ErrorAlert.show(e, controlledStage);
        }
    }

    @FXML
    public void kafkaConnectClick(ActionEvent actionEvent) {
        try {
            ClusterConfig selectedConfig = selectedCluster();
            if (StringUtils.isEmpty(selectedConfig.getkafkaConnectUrl())) {
                Optional<String> input = SystemUtils.showInputDialog("http://localhost:8083", "Add kafka connect url", "this cluster config is missing a kafka connect url please add it now", "kafka connect URL");
                if (!input.isPresent()) {
                    return;
                }
                input.ifPresent(url -> {
                    selectedConfig.setkafkaConnectUrl(url);
                    configHandler.saveConfigs();
                });
            }

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/kafkaConnectBrowser.fxml"));
            Parent root1 = fxmlLoader.load();
            KafkaConnectBrowserController controller = fxmlLoader.getController();
            controller.setup(selectedConfig, configHandler);
            Stage stage = new Stage();
            stage.getIcons().add(new Image(getClass().getResourceAsStream(ICONS_KAFKAESQUE_PNG_PATH)));
            stage.initOwner(controlledStage);
            stage.initModality(Modality.NONE);
            stage.setTitle("Browse Kafka Connect - " + selectedConfig.getIdentifier());
            stage.setScene(Main.createStyledScene(root1, -1, -1));
            stage.show();
            centerStageOnControlledStage(stage);
        } catch (Exception e) {
            ErrorAlert.show(e, controlledStage);
        }
    }

    @FXML
    public void kafkaConnectInstalledPluginClick(ActionEvent actionEvent) {
        try {
            ClusterConfig selectedConfig = selectedCluster();
            if (StringUtils.isEmpty(selectedConfig.getkafkaConnectUrl())) {
                Optional<String> input = SystemUtils.showInputDialog("http://localhost:8083", "Add kafka connect url", "this cluster config is missing a kafka connect url please add it now", "kafka connect URL");
                if (!input.isPresent()) {
                    return;
                }
                input.ifPresent(url -> {
                    selectedConfig.setkafkaConnectUrl(url);
                    configHandler.saveConfigs();
                });
            }

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/installedConnectorPluginsBrowser.fxml"));
            Parent root1 = fxmlLoader.load();
            InstalledConnectorPluginsController controller = fxmlLoader.getController();
            controller.setup(selectedConfig, configHandler);
            Stage stage = new Stage();
            stage.getIcons().add(new Image(getClass().getResourceAsStream(ICONS_KAFKAESQUE_PNG_PATH)));
            stage.initOwner(controlledStage);
            stage.initModality(Modality.NONE);
            stage.setTitle("Browse installed Kafka Connect plugins - " + selectedConfig.getIdentifier());
            stage.setScene(Main.createStyledScene(root1, -1, -1));
            stage.show();
            centerStageOnControlledStage(stage);
        } catch (Exception e) {
            ErrorAlert.show(e, controlledStage);
        }
    }

    @FXML
    public void crossClusterClick(ActionEvent actionEvent) {
        try {
            FXMLLoader fxmlLoader = injector.getInstance(FXMLLoader.class);
            fxmlLoader.setLocation(getClass().getResource("/fxml/crossClusterOperation.fxml"));
            Parent root1 = fxmlLoader.load();
            CrossClusterController controller = fxmlLoader.getController();
            controller.setup();
            Stage stage = new Stage();
            stage.getIcons().add(new Image(getClass().getResourceAsStream(ICONS_KAFKAESQUE_PNG_PATH)));
            stage.initOwner(controlledStage);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Cross Cluster Operations");
            stage.setScene(Main.createStyledScene(root1, 1000, 500));
            stage.show();
            centerStageOnControlledStage(stage);
        } catch (Exception e) {
            ErrorAlert.show(e, controlledStage);
        }
    }

    @FXML
    public void lagViewerClick(ActionEvent actionEvent) {
        try {
            FXMLLoader fxmlLoader = injector.getInstance(FXMLLoader.class);
            fxmlLoader.setLocation(getClass().getResource("/fxml/lagViewer.fxml"));
            Parent root1 = fxmlLoader.load();
            LagViewerController controller = fxmlLoader.getController();
            UUID consumerId = consumerHandler.registerConsumer(selectedCluster(), new TopicMessageTypeConfig(), new HashMap<>());
            KafkaConsumer consumer = consumerHandler.getConsumer(consumerId).orElseThrow(() -> new RuntimeException("Error getting consumer"));
            controller.setup(adminClient, consumer);
            Stage stage = new Stage();
            stage.getIcons().add(new Image(getClass().getResourceAsStream(ICONS_KAFKAESQUE_PNG_PATH)));
            stage.initOwner(controlledStage);
            stage.initModality(Modality.NONE);
            stage.setTitle("Lag Viewer - " + selectedCluster().getIdentifier());
            stage.setScene(Main.createStyledScene(root1, 1000, 500));
            stage.show();
            centerStageOnControlledStage(stage);
            stage.setOnCloseRequest(windowEvent -> {
                controller.stop();
                consumerHandler.deregisterConsumer(consumerId);
            });
        } catch (Exception e) {
            ErrorAlert.show(e, controlledStage);
        }
    }

    @FXML
    public void aclViewer(ActionEvent actionEvent) {
        try {
            FXMLLoader fxmlLoader = injector.getInstance(FXMLLoader.class);
            fxmlLoader.setLocation(getClass().getResource("/fxml/aclViewer.fxml"));
            Parent root1 = fxmlLoader.load();
            AclViewerController controller = fxmlLoader.getController();
            controller.setup(adminClient);
            Stage stage = new Stage();
            stage.getIcons().add(new Image(getClass().getResourceAsStream(ICONS_KAFKAESQUE_PNG_PATH)));
            stage.initOwner(controlledStage);
            stage.initModality(Modality.NONE);
            stage.setTitle("ACL Viewer - " + selectedCluster().getIdentifier());
            stage.setScene(Main.createStyledScene(root1, 1000, 500));
            stage.show();
            centerStageOnControlledStage(stage);
            stage.setOnCloseRequest(windowEvent -> controller.stop());
        } catch (Exception e) {
            ErrorAlert.show(e, controlledStage);
        }
    }

    private void getOldestMessages(TopicMessageTypeConfig topic, Map<String, String> consumerConfig) {
        runInDaemonThread(() -> {
            UUID consumerId = null;
            try {
                consumerId = consumerHandler.registerConsumer(selectedCluster(), topic, consumerConfig);
            } catch (MissingSchemaRegistryException e) {
                Platform.runLater(() -> ErrorAlert.show(e, controlledStage));
                return;
            }
            try {
                Map<Integer, AtomicLong> messagesConsumed = new HashMap<>();
                Platform.runLater(() -> backGroundTaskHolder.setBackGroundTaskDescription("preparing consumer..."));
                backGroundTaskHolder.setIsInProgress(true);
                subscribeOrAssignToSelectedPartition(topic, consumerId);
                Map<TopicPartition, Long> minOffsets = consumerHandler.getMinOffsets(consumerId);
                Map<TopicPartition, Long> maxOffsets = consumerHandler.getMaxOffsets(consumerId);
                consumerHandler.seekToOffset(consumerId, -1);
                Map<TopicPartition, Long> currentOffsets = consumerHandler.getCurrentOffsets(consumerId);
                PinTab tab = getActiveTabOrAddNew(topic, false);
                ObservableList<KafkaMessage> baseList = getAndClearBaseList(tab);
                Platform.runLater(() -> backGroundTaskHolder.setBackGroundTaskDescription("getting messages..."));
                consumerHandler.getConsumer(consumerId).ifPresent(topicConsumer -> {
                    while (!backGroundTaskHolder.getStopBackGroundTask() && !reachedMaxOffsetForAllPartitionsOrGotEnoughMessages(maxOffsets, minOffsets, currentOffsets, messagesConsumed, getNumberOfMessagesToConsume())) {
                        receiveMessages(messagesConsumed, currentOffsets, topicConsumer, getNumberOfMessagesToConsume(), baseList);
                    }
                });
            } finally {
                consumerHandler.deregisterConsumer(consumerId);
                backGroundTaskHolder.backgroundTaskStopped();
            }

        });
    }

    private ObservableList<KafkaMessage> getAndClearBaseList(PinTab tab) {
        if (tab == null) {
            return null;
        }
        ObservableList<KafkaMessage> baseList = ((MessagesTabContent) tab.getContent()).getMessageTableView().getBaseList();
        baseList.clear();
        return baseList;
    }

    private PinTab getActiveTabOrAddNew(TopicMessageTypeConfig topic, boolean isTrace) {
        PinTab tab;
        PinTab selectedTab = (PinTab) messageTabPane.getSelectionModel().getSelectedItem();
        String name = isTrace ? "trace " + topic.getName() : topic.getName();
        if (selectedTab == null || selectedTab.isPinned()) {
            tab = createTab(selectedCluster(), name);
            Platform.runLater(() -> messageTabPane.getTabs().add(tab));
        } else {
            tab = selectedTab;
            updateTabName(tab, selectedCluster(), name);
        }
        return tab;
    }

    private long getNumberOfMessagesToConsume() {
        return Long.parseLong(numberOfMessagesToGetField.getText());
    }

    private <KT, VT> void receiveMessages(Map<Integer, AtomicLong> messagesConsumedPerPartition, Map<TopicPartition, Long> currentOffsets, KafkaConsumer topicConsumer, long numberToConsume, ObservableList<KafkaMessage> baseList) {
        ConsumerRecords<KT, VT> records = topicConsumer.poll(Duration.ofSeconds(1));
        records.forEach(record -> {
            long numberConsumed = messagesConsumedPerPartition.computeIfAbsent(record.partition(), key -> new AtomicLong(0)).get();
            currentOffsets.put(new TopicPartition(record.topic(), record.partition()), record.offset());
            if (numberConsumed < numberToConsume) {
                convertAndAdd(record, baseList);
                messagesConsumedPerPartition.computeIfAbsent(record.partition(), key -> new AtomicLong(0)).incrementAndGet();
            }
        });
        Platform.runLater(() -> backGroundTaskHolder.setProgressMessage(String.format("Consumed %s messages", messagesConsumedPerPartition.values().stream().mapToLong(AtomicLong::get).sum())));
    }

    private void getNewestMessages(TopicMessageTypeConfig topic, Map<String, String> consumerConfig) {
        runInDaemonThread(() -> {
            UUID tempconsumerId = null;
            try {
                tempconsumerId = consumerHandler.registerConsumer(selectedCluster(), topic, consumerConfig);
            } catch (MissingSchemaRegistryException e) {
                Platform.runLater(() -> ErrorAlert.show(e, controlledStage));
                return;
            }
            UUID consumerId = tempconsumerId;
            try {
                Map<Integer, AtomicLong> messagesConsumed = new HashMap<>();
                Platform.runLater(() -> backGroundTaskHolder.setBackGroundTaskDescription("preparing consumer..."));
                backGroundTaskHolder.setIsInProgress(true);
                subscribeOrAssignToSelectedPartition(topic, consumerId);
                Map<TopicPartition, Long> minOffsets = consumerHandler.getMinOffsets(consumerId);
                Map<TopicPartition, Long> maxOffsets = consumerHandler.getMaxOffsets(consumerId);

                maxOffsets.forEach((topicPartition, maxOffset) -> {
                    long numberToLookBack = getNumberOfMessagesToConsume();
                    consumerHandler.getConsumer(consumerId).ifPresent(topicConsumer -> {
                        long offsetToSeekTo = maxOffset - numberToLookBack;
                        if (offsetToSeekTo > 0) {
                            Long minOffset = minOffsets.get(topicPartition);
                            if (offsetToSeekTo < minOffset) {
                                topicConsumer.seek(topicPartition, minOffset);
                            } else {
                                topicConsumer.seek(topicPartition, offsetToSeekTo);
                            }
                        } else {
                            topicConsumer.seekToBeginning(Collections.singletonList(topicPartition));
                        }
                    });

                });
                Map<TopicPartition, Long> currentOffsets = consumerHandler.getCurrentOffsets(consumerId);
                PinTab tab = getActiveTabOrAddNew(topic, false);
                ObservableList<KafkaMessage> baseList = getAndClearBaseList(tab);
                Platform.runLater(() -> backGroundTaskHolder.setBackGroundTaskDescription("getting messages..."));
                consumerHandler.getConsumer(consumerId).ifPresent(topicConsumer -> {
                    while (!backGroundTaskHolder.getStopBackGroundTask() && !reachedMaxOffsetForAllPartitions(maxOffsets, minOffsets, currentOffsets)) {
                        receiveMessages(messagesConsumed, currentOffsets, topicConsumer, getNumberOfMessagesToConsume(), baseList);
                    }
                });
            } finally {
                consumerHandler.deregisterConsumer(consumerId);
                backGroundTaskHolder.backgroundTaskStopped();
            }
        });
    }

    private void subscribeOrAssignToSelectedPartition(TopicMessageTypeConfig topic, UUID consumerId) {
        final Integer selectedPartition = partitionCombobox.getSelectionModel().getSelectedItem();
        if (selectedPartition >= 0) {
            consumerHandler.getConsumer(consumerId).ifPresent(topicConsumer -> topicConsumer.assign(Collections.singletonList(new TopicPartition(selectedTopic(), selectedPartition))));
        } else {
            consumerHandler.subscribe(consumerId, topic.getName());
        }
    }

    private <KT, VT> void getMessagesContinuously(TopicMessageTypeConfig topic, Map<String, String> consumerConfig) {
        UUID tempconsumerId = null;
        try {
            tempconsumerId = consumerHandler.registerConsumer(selectedCluster(), topic, consumerConfig);
        } catch (MissingSchemaRegistryException e) {
            Platform.runLater(() -> ErrorAlert.show(e, controlledStage));
            return;
        }
        UUID consumerId = tempconsumerId;
        runInDaemonThread(() -> {
            try {
                AtomicLong messagesConsumed = new AtomicLong(0);
                backGroundTaskHolder.setIsInProgress(true);
                subscribeOrAssignToSelectedPartition(topic, consumerId);
                consumerHandler.seekToOffset(consumerId, -2);
                PinTab tab = getActiveTabOrAddNew(topic, false);
                ObservableList<KafkaMessage> baseList = getAndClearBaseList(tab);
                consumerHandler.getConsumer(consumerId).ifPresent(topicConsumer -> {
                    while (!backGroundTaskHolder.getStopBackGroundTask()) {
                        ConsumerRecords<KT, VT> records = topicConsumer.poll(Duration.ofSeconds(1));
                        records.forEach(cr -> {
                            messagesConsumed.incrementAndGet();
                            convertAndAdd(cr, baseList);
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

    private <KT, VT> void trace(TopicMessageTypeConfig topic, Map<String, String> consumerConfig, Predicate<ConsumerRecord> predicate, Integer fasttracePartition, Long epoch, Predicate<ConsumerRecord> stopTraceInPartitionCondition) {
        runInDaemonThread(() -> {
            UUID consumerId = null;
            try {
                consumerId = consumerHandler.registerConsumer(selectedCluster(), topic, consumerConfig);
            } catch (MissingSchemaRegistryException e) {
                Platform.runLater(() -> ErrorAlert.show(e, controlledStage));
                return;
            }
            try {
                backGroundTaskHolder.setIsInProgress(true);
                List<TopicPartition> topicPatitions;
                if (fasttracePartition != null) {
                    topicPatitions = new ArrayList<>(Collections.singletonList(new TopicPartition(selectedTopic(), fasttracePartition)));
                } else {
                    topicPatitions = new ArrayList<>(adminClient.getTopicPatitions(selectedTopic()));
                }
                consumerHandler.getConsumer(consumerId).ifPresent(topicConsumer -> topicConsumer.assign(topicPatitions));

                AtomicLong messagesConsumed = new AtomicLong(0);
                AtomicLong messagesFound = new AtomicLong(0);
                Map<TopicPartition, Long> minOffsets = consumerHandler.getMinOffsets(consumerId);
                Map<TopicPartition, Long> maxOffsets = consumerHandler.getMaxOffsets(consumerId);
                if (epoch != null) {
                    consumerHandler.seekToTime(consumerId, epoch);
                } else {
                    consumerHandler.seekToOffset(consumerId, -1);
                }
                PinTab tab = getActiveTabOrAddNew(topic, true);
                ObservableList<KafkaMessage> baseList = getAndClearBaseList(tab);
                Map<TopicPartition, Long> currentOffsets = new HashMap<>();
                consumerHandler.getConsumer(consumerId).ifPresent(topicConsumer -> {
                    while (!backGroundTaskHolder.getStopBackGroundTask() && !reachedMaxOffsetForAllPartitions(maxOffsets, minOffsets, currentOffsets)) {
                        ConsumerRecords<KT, VT> records = topicConsumer.poll(Duration.ofSeconds(1));
                        records.forEach(cr -> {
                            if (!topicPatitions.contains(new TopicPartition(cr.topic(), cr.partition()))) {
                                return;
                            }
                            messagesConsumed.incrementAndGet();
                            currentOffsets.put(new TopicPartition(cr.topic(), cr.partition()), cr.offset());
                            if (predicate.test(cr)) {
                                convertAndAdd(cr, baseList);
                                messagesFound.incrementAndGet();
                            }
                            if (stopTraceInPartitionCondition != null && stopTraceInPartitionCondition.test(cr)) {
                                Optional<TopicPartition> first = topicPatitions.stream()
                                        .filter(topicPartition -> topicPartition.partition() == cr.partition())
                                        .findFirst();

                                first.ifPresent(topicPartition -> {
                                    topicPatitions.remove(topicPartition);
                                    maxOffsets.remove(topicPartition);
                                    minOffsets.remove(topicPartition);
                                    currentOffsets.remove(topicPartition);
                                    topicConsumer.assign(topicPatitions);
                                });
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
        if (topicDescription.isFailed()) {
            throw new RuntimeException("Failed to determine number of partitions!", topicDescription.getException());
        }
        int numberOfPartitions = topicDescription.getTopicDescription().partitions().size();

        return Utils.toPositive(Utils.murmur2(key.getBytes(StandardCharsets.UTF_8))) % numberOfPartitions;

    }

    private <KT, VT> void convertAndAdd(ConsumerRecord<KT, VT> cr, ObservableList<KafkaMessage> baseList) {
        KafkaMessage kafkaMessage = new KafkaMessage();
        kafkaMessage.setOffset(cr.offset());
        kafkaMessage.setPartition(cr.partition());
        kafkaMessage.setKey(cr.key() == null ? null : cr.key().toString());
        kafkaMessage.setValue(cr.value() == null ? null : cr.value().toString());

        if (cr.value() instanceof GenericData.Record recordValue) {
            kafkaMessage.setValueType(extractTypeFromGenericRecord(recordValue));
            kafkaMessage.getMetaData().add(new StringMetadata("Value Schema ID", extractSchemaIdFromGenericRecord(recordValue)));
        }
        if (cr.key() instanceof GenericData.Record recordKey) {
            kafkaMessage.setKeyType(extractTypeFromGenericRecord(recordKey));
        }

        kafkaMessage.setTimestamp(Instant.ofEpochMilli(cr.timestamp()).toString());
        kafkaMessage.setHeaders(FXCollections.observableArrayList(cr.headers().toArray()));

        kafkaMessage.getMetaData().add(new NumericMetadata("Serialized Key Size", (long) cr.serializedKeySize()));
        kafkaMessage.getMetaData().add(new NumericMetadata("Serialized Value Size", (long) cr.serializedValueSize()));

        Platform.runLater(() -> baseList.add(kafkaMessage));
    }

    private String extractSchemaIdFromGenericRecord(GenericData.Record genericRecord) {
        if (genericRecord == null || genericRecord.getSchema() == null) {
            return null;
        }
        Schema schema = genericRecord.getSchema();
        Map<String, Object> props = schema.getObjectProps();

        if (props == null)
            return "";

        return String.valueOf(schema.getObjectProps().get("schema-registry-schema-id"));
    }

    private String extractTypeFromGenericRecord(GenericData.Record genericRecord) {
        if (genericRecord == null || genericRecord.getSchema() == null) {
            return null;
        }
        Schema schema = genericRecord.getSchema();
        return schema.getNamespace() + "." + schema.getName();
    }

    private void getMessagesFromSpecificOffset(TopicMessageTypeConfig topic, Map<String, String> consumerConfig) {
        runInDaemonThread(() -> {
            UUID consumerId = null;
            try {
                consumerId = consumerHandler.registerConsumer(selectedCluster(), topic, consumerConfig);
            } catch (MissingSchemaRegistryException e) {
                ErrorAlert.show(e, controlledStage);
                return;
            }
            try {
                Map<Integer, AtomicLong> messagesConsumed = new HashMap<>();
                long specifiedOffset = Long.parseLong(specificOffsetTextField.getText());
                Platform.runLater(() -> backGroundTaskHolder.setBackGroundTaskDescription("preparing consumer..."));
                backGroundTaskHolder.setIsInProgress(true);
                subscribeOrAssignToSelectedPartition(topic, consumerId);
                Map<TopicPartition, Long> minOffsets = consumerHandler.getMinOffsets(consumerId);
                Map<TopicPartition, Long> maxOffsets = consumerHandler.getMaxOffsets(consumerId);
                consumerHandler.seekToOffset(consumerId, specifiedOffset);
                PinTab tab = getActiveTabOrAddNew(topic, false);
                ObservableList<KafkaMessage> baseList = getAndClearBaseList(tab);
                Map<TopicPartition, Long> currentOffsets = consumerHandler.getCurrentOffsets(consumerId);
                Platform.runLater(() -> backGroundTaskHolder.setBackGroundTaskDescription("getting messages..."));
                consumerHandler.getConsumer(consumerId).ifPresent(topicConsumer -> {
                    while (!backGroundTaskHolder.getStopBackGroundTask() && !reachedMaxOffsetForAllPartitionsOrGotEnoughMessages(maxOffsets, minOffsets, currentOffsets, messagesConsumed, getNumberOfMessagesToConsume())) {
                        receiveMessages(messagesConsumed, currentOffsets, topicConsumer, getNumberOfMessagesToConsume(), baseList);
                    }
                });
            } finally {
                consumerHandler.deregisterConsumer(consumerId);
                backGroundTaskHolder.backgroundTaskStopped();
            }
        });
    }

    private void getMessagesStartingFromInstant(TopicMessageTypeConfig topic, Map<String, String> consumerConfig) {
        runInDaemonThread(() -> {
            UUID consumerId = null;
            final Instant specifiedInstant = specificInstantPicker.getInstantValue();
            if (specifiedInstant == null) {
                return;
            }
            try {
                consumerId = consumerHandler.registerConsumer(selectedCluster(), topic, consumerConfig);
            } catch (MissingSchemaRegistryException e) {
                ErrorAlert.show(e, controlledStage);
                return;
            }
            try {
                Map<Integer, AtomicLong> messagesConsumed = new HashMap<>();
                Platform.runLater(() -> backGroundTaskHolder.setBackGroundTaskDescription("preparing consumer..."));
                backGroundTaskHolder.setIsInProgress(true);
                subscribeOrAssignToSelectedPartition(topic, consumerId);
                Map<TopicPartition, Long> minOffsets = consumerHandler.getMinOffsets(consumerId);
                Map<TopicPartition, Long> maxOffsets = consumerHandler.getMaxOffsets(consumerId);
                consumerHandler.seekToTime(consumerId, specifiedInstant.toEpochMilli());
                PinTab tab = getActiveTabOrAddNew(topic, false);
                ObservableList<KafkaMessage> baseList = getAndClearBaseList(tab);
                Map<TopicPartition, Long> currentOffsets = consumerHandler.getCurrentOffsets(consumerId);
                Platform.runLater(() -> backGroundTaskHolder.setBackGroundTaskDescription("getting messages..."));
                consumerHandler.getConsumer(consumerId).ifPresent(topicConsumer -> {
                    while (!backGroundTaskHolder.getStopBackGroundTask() && !reachedMaxOffsetForAllPartitionsOrGotEnoughMessages(maxOffsets, minOffsets, currentOffsets, messagesConsumed, getNumberOfMessagesToConsume())) {
                        receiveMessages(messagesConsumed, currentOffsets, topicConsumer, getNumberOfMessagesToConsume(), baseList);
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
                .noneMatch(maxOffset -> (maxOffset.getValue() > -1 && maxOffset.getValue() > minOffsets.get(maxOffset.getKey()) && (currentOffsets.get(maxOffset.getKey()) == null || (maxOffset.getValue() - 1 > (currentOffsets.get(maxOffset.getKey()) == null ? 0L : currentOffsets.get(maxOffset.getKey()))))));

    }

    private boolean reachedMaxOffsetForAllPartitionsOrGotEnoughMessages
            (Map<TopicPartition, Long> maxOffsets, Map<TopicPartition, Long> minOffsets, Map<TopicPartition, Long> currentOffsets, Map<Integer, AtomicLong> messagesConsumedPerPartition, long numberOfMessagesToConsume) {
        return maxOffsets.entrySet().stream().noneMatch(maxOffset -> {
            AtomicLong atomicLong = messagesConsumedPerPartition.get(maxOffset.getKey().partition());
            boolean notEnoughMessagesConsumed = (atomicLong == null ? 0L : atomicLong.get()) < numberOfMessagesToConsume;
            boolean notReachedMaxOffset = maxOffset.getValue() > -1 && maxOffset.getValue() > minOffsets.get(maxOffset.getKey()) && (currentOffsets.get(maxOffset.getKey()) == null || (maxOffset.getValue() - 1 > currentOffsets.get(maxOffset.getKey())));
            return notReachedMaxOffset && notEnoughMessagesConsumed;
        });

    }

    @FXML
    public void addClusterConfigClick(ActionEvent event) {
        ClusterConfigDialog.show().ifPresent(cc -> {
            clusterComboBox.getItems().add(cc);
            configHandler.saveConfigs();
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
                        configHandler.saveConfigs();
                    }
                });
    }

    @FXML
    public void editClusterConfigsClick(ActionEvent actionEvent) {
        ClusterConfig existingConfig = selectedCluster();
        ClusterConfigDialog.show(existingConfig).ifPresent(clusterConfig -> {
            existingConfig.update(clusterConfig);
            configHandler.saveConfigs();
        });
    }

    @FXML
    public void stopPolling(ActionEvent event) {
        backGroundTaskHolder.setStopBackGroundTask(true);
        backGroundTaskHolder.setBackGroundTaskDescription("stop polling");
    }

    private void showPublishMessageDialog(KafkaMessage kafkaMessage) {
        try {
            List<Integer> partitions = adminClient.getPatitions(selectedTopic());
            FXMLLoader fxmlLoader = injector.getInstance(FXMLLoader.class);
            fxmlLoader.setLocation(getClass().getResource("/fxml/publishMessage.fxml"));
            Parent root1 = fxmlLoader.load();
            PublisherController controller = fxmlLoader.getController();
            controller.setup(selectedCluster(), selectedTopic(), FXCollections.observableArrayList(partitions), kafkaMessage);
            Stage stage = new Stage();
            stage.getIcons().add(new Image(getClass().getResourceAsStream(ICONS_KAFKAESQUE_PNG_PATH)));
            stage.initOwner(controlledStage);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Publish Message");
            stage.setScene(Main.createStyledScene(root1, -1, -1));
            stage.setOnCloseRequest(event -> controller.cleanup());
            stage.show();
            centerStageOnControlledStage(stage);
        } catch (Exception e) {
            ErrorAlert.show(e, controlledStage);
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
            stage.getIcons().add(new Image(getClass().getResourceAsStream(ICONS_KAFKAESQUE_PNG_PATH)));
            stage.initOwner(controlledStage);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Create Topic");
            stage.setScene(Main.createStyledScene(root1, -1, -1));
            stage.show();
            centerStageOnControlledStage(stage);
        } catch (Exception e) {
            ErrorAlert.show(e, controlledStage);
        }
    }

    private void showDescribeTopicDialog(String topic) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/describeTopic.fxml"));
            Parent root1 = fxmlLoader.load();
            DescribeTopicController controller = fxmlLoader.getController();
            final DescribeTopicWrapper describeTopicWrapper = adminClient.describeTopic(topic);
            if (!describeTopicWrapper.isFailed()) {
                controller.setup(describeTopicWrapper);
                Stage stage = new Stage();
                stage.getIcons().add(new Image(getClass().getResourceAsStream(ICONS_KAFKAESQUE_PNG_PATH)));
                stage.initOwner(controlledStage);
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Topic Description");
                Scene styledScene = Main.createStyledScene(root1, -1, -1);
                stage.setScene(styledScene);
                stage.show();
                centerStageOnControlledStage(stage);
            } else {
                final Exception exception = describeTopicWrapper.getException();
                ErrorAlert.show("Failed to describe topic", "Topic description failed: " + exception.getClass().getName(), exception.getMessage(), exception, controlledStage, true);
            }
        } catch (Exception e) {
            ErrorAlert.show(e, controlledStage);
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

    // Experimental Area

    private void showJsonDiffDialog(KafkaMessage source, KafkaMessage target) {
        try {
            FXMLLoader fxmlLoader = injector.getInstance(FXMLLoader.class);
            fxmlLoader.setLocation(getClass().getResource("/fxml/MessageDiffView.fxml"));
            Parent root1 = fxmlLoader.load();
            MessageDiffView controller = fxmlLoader.getController();
            controller.setup(source, target);
            Stage stage = new Stage();
            stage.getIcons().add(new Image(getClass().getResourceAsStream(ICONS_KAFKAESQUE_PNG_PATH)));
            stage.initOwner(controlledStage);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Json Diff");
            stage.setScene(Main.createStyledScene(root1, -1, -1));
            stage.show();
            centerStageOnControlledStage(stage);
        } catch (Exception e) {
            ErrorAlert.show(e, controlledStage);
        }
    }

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
                ErrorAlert.show(e, controlledStage);
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
                                Platform.runLater(() -> ErrorAlert.show(e, controlledStage));
                            }
                        }
                        Platform.runLater(() -> backGroundTaskHolder.setProgressMessage("Created Topic " + createdTopics.size() + " of " + topicsToCreate.size() + " (" + alreadyExistedTopics.size() + " already existed)"));
                    });
                    Platform.runLater(() -> TopicTemplateAppliedAlert.show(createdTopics, alreadyExistedTopics, topicListView.getScene().getWindow()));
                } finally {
                    backGroundTaskHolder.backgroundTaskStopped();
                }
            });
        }
    }

    @FXML
    public void showSettingsDialog(ActionEvent event) {
        Map<String, String> settingsProperties = configHandler.getSettingsProperties();
        SettingsDialog.show(settingsProperties).ifPresent(
                settingsProperties1 -> {
                    try {
                        configHandler.setSettingsProperties(settingsProperties1);
                    } catch (IOException e) {
                        ErrorAlert.show(e);
                    }
                }
        );
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
                Map<String, UUID> topicToProducerMap = new HashMap<>();
                try {
                    backGroundTaskHolder.setIsInProgress(true);
                    List<File> listedFiles = Arrays.asList(Objects.requireNonNull(selectedFolder.listFiles()));
                    Map<String, String> replacementMap = new HashMap<>();
                    List<KafkaMessagBookWrapper> messagesToSend = new ArrayList<>();
                    Platform.runLater(() -> backGroundTaskHolder.setBackGroundTaskDescription("Playing Message Book: scanning messages"));
                    listedFiles.forEach(file -> {
                        if (!topicListView.getBaseList().contains(file.getName())) {
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
                                try {
                                    UUID producerId = topicToProducerMap.computeIfAbsent(message.getTargetTopic(), targetTopic -> {
                                        try {
                                            return producerHandler.registerProducer(selectedCluster(), targetTopic);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                                    producerHandler.sendMessage(producerId, message.getTargetTopic(), message.getPartition() == -1 ? null : message.getPartition(), message.getKey(), message.getValue(), message.getKeyType(), message.getValueType());
                                    Platform.runLater(() -> backGroundTaskHolder.setProgressMessage("published " + counter.incrementAndGet() + " messages"));
                                } catch (InterruptedException | ExecutionException | TimeoutException | IOException |
                                         RestClientException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                } catch (Exception e) {
                    Platform.runLater(() -> ErrorAlert.show(e, controlledStage));
                } finally {
                    stopWatch.stop();
                    topicToProducerMap.values().forEach(uuid -> producerHandler.deregisterProducer(uuid));
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
                    .withType(KafkaMessage.class)
                    .build().parse();
            messagesToSend.addAll(messages.stream().map(message -> new KafkaMessagBookWrapper(playFile.getName(), message))
                    .toList());
        } catch (FileNotFoundException e) {
            Platform.runLater(() -> ErrorAlert.show(e, controlledStage));
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

    private void centerStageOnControlledStage(Stage stage) {
        stage.setX(controlledStage.getX() + controlledStage.getWidth() / 2 - stage.getWidth() / 2);
        stage.setY(controlledStage.getY() + controlledStage.getHeight() / 2 - stage.getHeight() / 2);
    }

    private PinTab createTab(ClusterConfig clusterConfig, String name) {

        MessagesTabContent messagesTabContent = new MessagesTabContent();

        messagesTabContent.getMessageTableView().setRowFactory(
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
                    MenuItem jsonDiff = new MenuItem("Json Diff");
                    jsonDiff.setGraphic(new FontIcon(FontAwesome.EXCHANGE));
                    jsonDiff.disableProperty().bind(Bindings.createBooleanBinding(() -> messagesTabContent.getMessageTableView().getSelectionModel().getSelectedItems() != null && messagesTabContent.getMessageTableView().getSelectionModel().getSelectedItems().size() != 2, messagesTabContent.getMessageTableView().getSelectionModel().getSelectedItems()));
                    jsonDiff.setOnAction(event -> {
                        final ObservableList<KafkaMessage> selectedItems = messagesTabContent.getMessageTableView().getSelectionModel().getSelectedItems();
                        if (selectedItems != null && selectedItems.size() == 2) {
                            showJsonDiffDialog(selectedItems.get(0), selectedItems.get(1));
                        } else {
                            ErrorAlert.show("Unsupported selection", "Selection has to be exactly 2 message", null, null, controlledStage, false);
                        }

                    });
                    rowMenu.getItems().addAll(openinPublisher, openAsTxt, openAsJson, jsonDiff);

                    // only display context menu for non-null items:
                    row.contextMenuProperty().bind(
                            Bindings.when(Bindings.isNotNull(row.itemProperty()))
                                    .then(rowMenu)
                                    .otherwise((ContextMenu) null));
                    return row;
                });
        messagesTabContent.getMessageTableView().getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        messagesTabContent.getMessageTableView().getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedMessage = newValue;
            updateKeyValueTextArea(selectedMessage, formatJsonToggle.isSelected());
        });

        messagesTabContent.getMessageTableView().focusedProperty().addListener((observableValue, oldValue, newValue) -> {
            KafkaMessage selectedItem = messagesTabContent.getMessageTableView().getSelectionModel().getSelectedItem();
            updateKeyValueTextArea(selectedItem, formatJsonToggle.isSelected());
        });

        return new PinTab(clusterConfig.getIdentifier() + " - " + name, messagesTabContent);
    }

    private void updateTabName(PinTab tab, ClusterConfig clusterConfig, String name) {
        Platform.runLater(() -> tab.setText(clusterConfig.getIdentifier() + " - " + name));
    }

    @FXML
    public void aboutClick(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/about.fxml"));
            Parent root1 = fxmlLoader.load();
            AboutController controller = fxmlLoader.getController();
            controller.setup(versionInfoHandler, hostServices);
            Stage stage = new Stage();
            stage.getIcons().add(new Image(getClass().getResourceAsStream(ICONS_KAFKAESQUE_PNG_PATH)));
            stage.initOwner(controlledStage);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("About KafkaEsque");
            stage.setScene(Main.createStyledScene(root1, -1, -1));
            stage.setResizable(false);
            stage.show();
            centerStageOnControlledStage(stage);
        } catch (Exception e) {
            ErrorAlert.show(e, controlledStage);
        }
    }

    private EventHandler<? super KeyEvent> generateHeaderTableEventHandler() {
        Map<KeyCodeCombination, Function<Header, String>> copyCombinations = Map.of(
                new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN), header -> new String(header.value(), StandardCharsets.UTF_8),
                new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN), Header::key
        );

        return SystemUtils.generateTableCopySelectedItemCopyEventHandler(headerTableView, copyCombinations);
    }

    private EventHandler<? super KeyEvent> generateMetadataTableEventHandler() {
        Map<KeyCodeCombination, Function<MessageMetaData, String>> copyCombinations = Map.of(
                new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN), metadata -> metadata.valueAsString().getValue(),
                new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN), metadata -> metadata.nameProperty().getName()
        );

        return SystemUtils.generateTableCopySelectedItemCopyEventHandler(metdataTableView, copyCombinations);
    }


    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    private String buildToolTip() {
        return String.format("The following KeyCombinations let you copy data from the selected element in the metadata and header table%n" +
                new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN).getDisplayText() + " - copy Key/Name%n" +
                new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN).getDisplayText() + " - copy Value%n");
    }
}
