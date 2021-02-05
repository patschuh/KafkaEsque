package at.esque.kafka.acl.viewer;

import at.esque.kafka.cluster.KafkaesqueAdminClient;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourceType;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

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
    TableColumn<Acl, String> resourceTypeColumn;
    @FXML
    TableColumn<Acl, String> resourceNameColumn;
    @FXML
    TableColumn<Acl, String> patternTypeColumn;
    @FXML
    TableColumn<Acl, String> principalColumn;
    @FXML
    TableColumn<Acl, String> operationColumn;
    @FXML
    TableColumn<Acl, String> permissionTypeColumn;
    @FXML
    TableColumn<Acl, String> hostColumn;

    private KafkaesqueAdminClient adminClient;

    private BooleanProperty refreshRunning = new SimpleBooleanProperty(false);

    @FXML
    public void initialize() {
        resourceTypeCombo.setItems(FXCollections.observableArrayList(ResourceType.values()));
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

                                if (item instanceof Acl) {
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

    public void setup(KafkaesqueAdminClient adminClient) {
        this.adminClient = adminClient;
    }

    @FXML
    private void startSearch(ActionEvent actionEvent) {
        runInDaemonThread(() -> {
            List<Acl> aclList = new ArrayList<>();
            Platform.runLater(() -> {
                refreshRunning.setValue(true);
                resultView.setItems(FXCollections.observableArrayList(aclList));
            });
            adminClient.getACLs(resourceTypeCombo.getValue(), resourcePatternCombo.getValue(), resourceName.getText())
                    .forEach(acl -> Platform.runLater(() -> aclList.add(new Acl(acl))));
            Platform.runLater(() -> refreshRunning.setValue(false));
        });
    }


    public void stop() {
        adminClient = null;
    }

    private void runInDaemonThread(Runnable runnable) {
        Thread daemonThread = new Thread(runnable);
        daemonThread.setDaemon(true);
        daemonThread.start();
    }

}
