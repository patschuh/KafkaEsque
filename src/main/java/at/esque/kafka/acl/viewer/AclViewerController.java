package at.esque.kafka.acl.viewer;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.cluster.KafkaesqueAdminClient;
import at.esque.kafka.controls.FilterableListView;
import at.esque.kafka.controls.LagViewerCellContent;
import at.esque.kafka.lag.viewer.Lag;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.*;

public class AclViewerController {

    @FXML
    ComboBox<ResourceType> resourceTypeCombo;

    @FXML
    TextField resourceName;

    @FXML
    ComboBox<PatternType> resourcePatternCombo;

    @FXML
    TableView<Acl> resultView;

    @FXML
    TableColumn<Acl,String> resourceTypeColumn;
    @FXML
    TableColumn<Acl,String> resourceNameColumn;
    @FXML
    TableColumn<Acl,String> patternTypeColumn;
    @FXML
    TableColumn<Acl,String> principalColumn;
    @FXML
    TableColumn<Acl,String> operationColumn;
    @FXML
    TableColumn<Acl,String> permissionTypeColumn;
    @FXML
    TableColumn<Acl,String> hostColumn;

    private KafkaesqueAdminClient adminClient;

    private KafkaConsumer kafkaConsumer;

    private BooleanProperty refreshRunning = new SimpleBooleanProperty(false);

    @FXML
    public void initialize() {
        resourceTypeCombo.setItems(FXCollections.observableArrayList( ResourceType.values()));
        resourcePatternCombo.setItems(FXCollections.observableArrayList(PatternType.values()));

        resourceTypeCombo.getSelectionModel().select(ResourceType.ANY);
        resourcePatternCombo.getSelectionModel().select(PatternType.ANY);

        //Remove Unkown those are not support in Admin Client for the Query
        resourceTypeCombo.getItems().remove(ResourceType.UNKNOWN);
        resourcePatternCombo.getItems().remove(PatternType.UNKNOWN);


        resourceTypeColumn.setCellValueFactory(new PropertyValueFactory<>("resourceType"));
        resourceNameColumn.setCellValueFactory(new PropertyValueFactory<>("resourceName"));
        patternTypeColumn.setCellValueFactory(new PropertyValueFactory<>("patternType"));
        principalColumn.setCellValueFactory(new PropertyValueFactory<>("principal"));
        operationColumn.setCellValueFactory(new PropertyValueFactory<>("operation"));
        permissionTypeColumn.setCellValueFactory(new PropertyValueFactory<>("permissionType"));
        hostColumn.setCellValueFactory(new PropertyValueFactory<>("host"));

        MenuItem mi1 = new MenuItem("Menu item 1");
        mi1.setOnAction((ActionEvent event) -> {
            System.out.println("Menu item 1");
            Object item = resultView.getSelectionModel().getSelectedItem();
            System.out.println("Selected item: " + item);
        });

        // Add Delete Context Menu to Table Rows
        resultView.setRowFactory(
                new Callback<TableView<Acl>, TableRow<Acl>>() {
                    @Override
                    public TableRow<Acl> call(TableView<Acl> tableView) {
                        final TableRow<Acl> row = new TableRow<>();
                        final ContextMenu rowMenu = new ContextMenu();

                        MenuItem deleteItem = new MenuItem("Delete");
                        deleteItem.setGraphic(new FontIcon(FontAwesome.TRASH));
                        deleteItem.setOnAction(new EventHandler<ActionEvent>() {

                            @Override
                            public void handle(ActionEvent event) {
                                Object item = resultView.getSelectionModel().getSelectedItem();

                                if (item instanceof Acl)
                                {
                                    Acl selectedAcl = (Acl) item;
                                    adminClient.deleteAcl(selectedAcl.getAclBinding());
                                }

                                startSearch(null);

                            }
                        });
                        rowMenu.getItems().addAll(deleteItem);
                        row.contextMenuProperty().set(rowMenu);

                        return row;
                    }
                });

    }

    public void setup(KafkaesqueAdminClient adminClient, KafkaConsumer kafkaConsumer) {
        this.adminClient = adminClient;
        this.kafkaConsumer = kafkaConsumer;

        this.adminClient = adminClient;
    }

    @FXML
    private void startSearch(ActionEvent actionEvent) {
        runInDaemonThread(() -> {
                Platform.runLater(() -> {
                    refreshRunning.setValue(true);
                    List<Acl> aclList = new ArrayList<>();

                    adminClient.getACLs(resourceTypeCombo.getValue(), resourcePatternCombo.getValue(), resourceName.getText())
                            .forEach(acl -> aclList.add(new Acl(acl)));

                    resultView.setItems(FXCollections.observableArrayList(aclList));
                });
            Platform.runLater(() -> refreshRunning.setValue(false));
        });
    }


    public void stop(){
        kafkaConsumer = null;
    }

    private void runInDaemonThread(Runnable runnable) {
        Thread daemonThread = new Thread(runnable);
        daemonThread.setDaemon(true);
        daemonThread.start();
    }

}
