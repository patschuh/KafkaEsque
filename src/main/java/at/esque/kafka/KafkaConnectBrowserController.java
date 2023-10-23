package at.esque.kafka;

import at.esque.kafka.alerts.ConfirmationAlert;
import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.alerts.SuccessAlert;
import at.esque.kafka.alerts.WarningAlert;
import at.esque.kafka.cluster.ClusterConfig;
import at.esque.kafka.connect.KafkaesqueConnectClient;
import at.esque.kafka.connect.Status;
import at.esque.kafka.connect.utils.ConnectUtil;
import at.esque.kafka.controls.FilterableListView;
import at.esque.kafka.controls.KafkaEsqueCodeArea;
import at.esque.kafka.handlers.ConfigHandler;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Map;
import java.util.function.Function;


public class KafkaConnectBrowserController {

    private static final String RIGHT_PANE_CONNECTOR_ACTION_PAUSE = "pause connector";
    private static final String RIGHT_PANE_CONNECTOR_ACTION_RESUME = "resume connector";
    private static final String RIGHT_PANE_CONNECTOR_ACTION_RESTART = "restart connector";

    @FXML
    private Tooltip helpIconToolTip;

    @FXML
    private FilterableListView<String> connectorListView;
    @FXML
    private KafkaEsqueCodeArea connectorConfigTextArea;

    @FXML
    private Label connectorStatus;

    @FXML
    private Button pauseButton;

    @FXML
    private Button resumeButton;

    @FXML
    private TableView taskTableView;

    @FXML
    private TableColumn<Status.TaskStatus, String> taskIdColumn;
    @FXML
    private TableColumn<Status.TaskStatus, String> taskStatusColumn;
    @FXML
    private TableColumn<Status.TaskStatus, String> workerIdColumn;
    @FXML
    private TableColumn<Status.TaskStatus, String> traceColumn;


    private String selectedConnectorInRightPane;

    private KafkaesqueConnectClient kafkaesqueConnectClient;

    public void setup(ClusterConfig selectedConfig, ConfigHandler configHandler) {
        kafkaesqueConnectClient = new KafkaesqueConnectClient(selectedConfig.getkafkaConnectUrl(), selectedConfig.getkafkaConnectBasicAuthUser(), selectedConfig.getkafkaConnectBasicAuthPassword(), selectedConfig.isKafkaConnectuseSsl(), configHandler.getSslProperties(selectedConfig));

        // Setup Connector List
        connectorListView.getListView().setCellFactory(param -> connectorListCellFactory());
        connectorListView.setListComparator(String::compareTo);

        connectorListView.getListView().getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateRightPane(newValue);
        });

        //Setup Task Table
        taskIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        taskStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        workerIdColumn.setCellValueFactory(new PropertyValueFactory<>("workerId"));
        traceColumn.setCellValueFactory(new PropertyValueFactory<>("trace"));

        taskTableView.setRowFactory(
                new Callback<TableView<Status.TaskStatus>, TableRow<Status.TaskStatus>>() {
                    @Override
                    public TableRow<Status.TaskStatus> call(TableView<Status.TaskStatus> tableView) {
                        final TableRow<Status.TaskStatus> row = new TableRow<>();
                        final ContextMenu rowMenu = new ContextMenu();

                        MenuItem restartItem = new MenuItem("Restart Task");
                        restartItem.setGraphic(new FontIcon(FontAwesome.RETWEET));
                        restartItem.setOnAction(new EventHandler<ActionEvent>() {

                            @Override
                            public void handle(ActionEvent event) {
                                try {
                                    Object item = taskTableView.getSelectionModel().getSelectedItem();

                                    if (item instanceof Status.TaskStatus) {
                                        Status.TaskStatus taskStatus = (Status.TaskStatus) item;

                                        kafkaesqueConnectClient.restartConnectorTask(selectedConnectorInRightPane, taskStatus.getId());
                                    }

                                    updateRightPane(selectedConnectorInRightPane);
                                } catch (Exception e) {
                                    ErrorAlert.show(e, getWindow());
                                }

                            }
                        });
                        rowMenu.getItems().addAll(restartItem);
                        row.contextMenuProperty().set(rowMenu);
                        return row;
                    }
                });

        taskTableView.setOnKeyPressed(generateMessageTableCopyEventHandler());
        helpIconToolTip.setText(buildToolTip());
    }

    private Window getWindow() {
        return connectorConfigTextArea.getScene().getWindow();
    }

    public void updateRightPane(String selectedConnector) {
        try {
            if (selectedConnector != null) {
                Map<String, String> connectorConfig = kafkaesqueConnectClient.getConnectorConfig(selectedConnector);
                connectorConfigTextArea.setText(ConnectUtil.buildConfigString(connectorConfig, ConnectUtil.PARAM_BLACK_LIST_VIEW));

                Status status = kafkaesqueConnectClient.getConnectorStatus(selectedConnector);
                connectorStatus.setText(status.getStatus());

                updateDisableFlagOfConnectorActionButtonStatus(status.getStatus());
                selectedConnectorInRightPane = selectedConnector;

                taskTableView.setItems(FXCollections.observableArrayList(status.getTaskStatusList()));
            } else {
                connectorConfigTextArea.setText("");
                connectorStatus.setText("");
                updateDisableFlagOfConnectorActionButtonStatus(null);
                selectedConnectorInRightPane = null;
                taskTableView.setItems(null);
            }
        } catch (Exception e) {
            ErrorAlert.show(e, getWindow());
        }


    }


    public void addConnector(ActionEvent actionEvent) {
        showConnectConfigDialog(null);
    }


    public void refreshConnectors(ActionEvent actionEvent) {
        try {
            connectorListView.getListView().getSelectionModel().select(null);
            connectorListView.setItems(FXCollections.observableArrayList(kafkaesqueConnectClient.getConnectors()));
        } catch (Exception e) {
            ErrorAlert.show(e, getWindow());
        }
    }

    private void showConnectConfigDialog(String selectedConnector) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/createConnector.fxml"));
            Parent root1 = fxmlLoader.load();
            CreateConnectorController controller = fxmlLoader.getController();
            Stage stage = new Stage();
            controller.setup(selectedConnector, kafkaesqueConnectClient, stage);
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/kafkaesque.png")));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add Connector");
            stage.setScene(Main.createStyledScene(root1, -1, -1));
            stage.setOnCloseRequest(event -> controller.cleanup());
            stage.show();
        } catch (Exception e) {
            ErrorAlert.show(e, getWindow());
        }
    }

    private ListCell<String> connectorListCellFactory() {
        ListCell<String> cell = new ListCell<>();

        ContextMenu contextMenu = new ContextMenu();

        MenuItem deleteItem = new MenuItem();
        deleteItem.setGraphic(new FontIcon(FontAwesome.TRASH));
        deleteItem.textProperty().set("delete");
        deleteItem.setOnAction(event -> {
            if (ConfirmationAlert.show("Delete Connector", "Connector [" + cell.itemProperty().get() + "] will be deleted.", "Are you sure you want to delete this connector", getWindow())) {
                try {
                    boolean result = kafkaesqueConnectClient.deleteConnector(cell.itemProperty().get());

                    if (result == true) {
                        SuccessAlert.show("Delete Connector", null, "Connector [" + cell.itemProperty().get() + "] deleted.", getWindow());
                    } else {
                        WarningAlert.show("Delete Connector", null, "It wasn't possible to delete the connector", getWindow());
                    }
                } catch (Exception e) {
                    ErrorAlert.show(e, getWindow());
                }
            }
        });

        MenuItem configItem = new MenuItem();
        configItem.setGraphic(new FontIcon(FontAwesome.COG));
        configItem.textProperty().set("configure");
        configItem.setOnAction(event -> {
            showConnectConfigDialog(cell.itemProperty().get());
        });

        contextMenu.getItems().add(configItem);
        contextMenu.getItems().add(deleteItem);

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


    private void updateDisableFlagOfConnectorActionButtonStatus(String connectorStatus) {
        if (connectorStatus == null)
            connectorStatus = "";

        switch (connectorStatus) {
            case "RUNNING":
                pauseButton.setDisable(false);
                resumeButton.setDisable(true);
                break;
            case "PAUSED":
                pauseButton.setDisable(true);
                resumeButton.setDisable(false);
                break;
            default:
                pauseButton.setDisable(true);
                resumeButton.setDisable(true);
        }
    }

    @FXML
    public void pauseConnectorClick(ActionEvent actionEvent) {
        rightPaneConnectorAction(RIGHT_PANE_CONNECTOR_ACTION_PAUSE);
    }

    @FXML
    public void resumeConnectorClick(ActionEvent actionEvent) {
        rightPaneConnectorAction(RIGHT_PANE_CONNECTOR_ACTION_RESUME);
    }

    @FXML
    public void restartConnectorClick(ActionEvent actionEvent) {
        rightPaneConnectorAction(RIGHT_PANE_CONNECTOR_ACTION_RESTART);
    }

    private void rightPaneConnectorAction(String action) {
        try {

            if (selectedConnectorInRightPane == null)
                return;

            boolean result = false;

            switch (action) {
                case RIGHT_PANE_CONNECTOR_ACTION_PAUSE:
                    result = kafkaesqueConnectClient.pauseConnector(selectedConnectorInRightPane);
                    break;
                case RIGHT_PANE_CONNECTOR_ACTION_RESUME:
                    result = kafkaesqueConnectClient.resumeConnector(selectedConnectorInRightPane);
                    break;
                case RIGHT_PANE_CONNECTOR_ACTION_RESTART:
                    result = kafkaesqueConnectClient.restartConnector(selectedConnectorInRightPane);
                    break;
            }

            if (result != true) {
                WarningAlert.show("Connector Action", null, String.format("Connector action '%s' returned fales from API!", action), getWindow());
            }

            //Refresh view
            updateRightPane(selectedConnectorInRightPane);
        } catch (Exception e) {
            ErrorAlert.show(e, getWindow());
        }
    }

    private EventHandler<? super KeyEvent> generateMessageTableCopyEventHandler() {
        Map<KeyCodeCombination, Function<Status.TaskStatus, String>> copyCombinations = Map.of(
                new KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN), taskStatus -> Integer.toString(taskStatus.getId()),
                new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), Status.TaskStatus::getStatus,
                new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN), Status.TaskStatus::getWorkerId,
                new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN), Status.TaskStatus::getTrace
        );

        return SystemUtils.generateTableCopySelectedItemCopyEventHandler(taskTableView, copyCombinations);
    }

    private String buildToolTip() {
        return String.format("The following KeyCombinations let you copy data from the selected element in the tasks table%n" +
                new KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN).getDisplayText() + " - copy task id%n" +
                new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN).getDisplayText() + " - copy task status%n" +
                new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN).getDisplayText() + " - copy worker id%n" +
                new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN).getDisplayText() + " - copy trace");
    }
}
