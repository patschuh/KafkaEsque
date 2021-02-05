package at.esque.kafka;

import at.esque.kafka.acl.viewer.Acl;
import at.esque.kafka.alerts.ConfirmationAlert;
import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.alerts.SuccessAlert;
import at.esque.kafka.alerts.WarningAlert;
import at.esque.kafka.cluster.ClusterConfig;
import at.esque.kafka.connect.KafkaesqueConnectClient;
import at.esque.kafka.connect.Status;
import at.esque.kafka.controls.FilterableListView;
import at.esque.kafka.controls.KafkaEsqueCodeArea;
import at.esque.kafka.handlers.ConfigHandler;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import org.apache.zookeeper.data.Stat;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;
import scala.Int;

import java.util.Map;


public class KafkaConnectBrowserController {

    private static final String RIGHT_PANE_CONNECTOR_ACTION_PAUSE   = "pause connector";
    private static final String RIGHT_PANE_CONNECTOR_ACTION_RESUME  = "resume connector";
    private static final String RIGHT_PANE_CONNECTOR_ACTION_RESTART = "restart connector";

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
        kafkaesqueConnectClient = new KafkaesqueConnectClient(selectedConfig.getkafkaConnectUrl(), selectedConfig.getkafkaConnectBasicAuthUser(), selectedConfig.getkafkaConnectBasicAuthPassword(), configHandler.getSslProperties(selectedConfig));

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
                                } catch (Exception e)
                                {
                                    ErrorAlert.show(e);
                                }

                            }
                        });
                        rowMenu.getItems().addAll(restartItem);
                        row.contextMenuProperty().set(rowMenu);
                        return row;
                    }
                });



    }

    public void updateRightPane(String selectedConnector)
    {
        try {
            if(selectedConnector != null) {
                Map<String, String> connectorConfig = kafkaesqueConnectClient.getConnectorConfig(selectedConnector);
                connectorConfigTextArea.setText(buildConfigString(connectorConfig));

                Status status = kafkaesqueConnectClient.getConnectorStatus(selectedConnector);
                connectorStatus.setText(status.getStatus());

                updateDisableFlagOfConnectorActionButtonStatus(status.getStatus());
                selectedConnectorInRightPane = selectedConnector;

                taskTableView.setItems(FXCollections.observableArrayList(status.getTaskStatusList()));
            }
            else
            {
                connectorConfigTextArea.setText("");
                connectorStatus.setText("");
                updateDisableFlagOfConnectorActionButtonStatus(null);
                selectedConnectorInRightPane = null;
                taskTableView.setItems(null);
            }
        } catch (Exception e) {
            ErrorAlert.show(e);
        }


    }


    public void addConnector(ActionEvent actionEvent) {
        //TODO
    }


    public void refreshConnectors(ActionEvent actionEvent) {
        try {
            connectorListView.getListView().getSelectionModel().select(null);
            connectorListView.setItems(FXCollections.observableArrayList(kafkaesqueConnectClient.getConnectors()));
        } catch (Exception e) {
            ErrorAlert.show(e);
        }
    }

    private ListCell<String> connectorListCellFactory() {
        ListCell<String> cell = new ListCell<>();

        ContextMenu contextMenu = new ContextMenu();

        MenuItem deleteItem = new MenuItem();
        deleteItem.setGraphic(new FontIcon(FontAwesome.TRASH));
        deleteItem.textProperty().set("delete");
        deleteItem.setOnAction(event -> {
                if (ConfirmationAlert.show("Delete Connector", "Connector [" + cell.itemProperty().get() + "] will deleted.", "Are you sure you want to delete this connector")) {
                    try {
                        boolean result = kafkaesqueConnectClient.deleteConnector(cell.itemProperty().get());

                        if(result == true) {
                            SuccessAlert.show("Delete Connector", null, "Connector [" + cell.itemProperty().get() + "] deleted.");
                        } else
                        {
                            WarningAlert.show( "Delete Connector", null, "It wasn't possible to delete the connector");
                        }
                    } catch (Exception e) {
                        ErrorAlert.show(e);
                    }
                }
            });

        MenuItem configItem = new MenuItem();
        configItem.setGraphic(new FontIcon(FontAwesome.COG));
        configItem.textProperty().set("configure");
        configItem.setOnAction(event -> {
          //  configureConnector();
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

    private String buildConfigString(Map<String,String> configMap)
    {
        StringBuilder configString = new StringBuilder();
        boolean firstRow = true;
        for(String entry : configMap.keySet())
        {
            if (firstRow == false)
                configString.append(System.getProperty("line.separator"));
            configString.append(entry);
            configString.append(":");
            configString.append(configMap.get(entry));
            firstRow = false;
        }

        return configString.toString();
    }

    private void updateDisableFlagOfConnectorActionButtonStatus(String connectorStatus)
    {
        if (connectorStatus == null)
            connectorStatus = "";

        switch(connectorStatus){
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
    public void pauseConnectorClick(ActionEvent actionEvent)
    {
        rightPaneConnectorAction(RIGHT_PANE_CONNECTOR_ACTION_PAUSE);
    }

    @FXML
    public void resumeConnectorClick(ActionEvent actionEvent)
    {
        rightPaneConnectorAction(RIGHT_PANE_CONNECTOR_ACTION_RESUME);
    }

    @FXML
    public void restartConnectorClick(ActionEvent actionEvent)
    {
        rightPaneConnectorAction(RIGHT_PANE_CONNECTOR_ACTION_RESTART);
    }

    private void rightPaneConnectorAction(String action)
    {
        try {

            if (selectedConnectorInRightPane == null)
                return;

            boolean result = false;

            switch(action){
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

            if (result != true)
            {
                WarningAlert.show("Connector Action", null, String.format("Connector action '%s' returned fales from API!",action));
            }

            //Refresh view
            updateRightPane(selectedConnectorInRightPane);
        } catch (Exception e) {
            ErrorAlert.show(e);
        }
    }
}
