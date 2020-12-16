package at.esque.kafka;

import at.esque.kafka.alerts.ConfirmationAlert;
import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.cluster.ClusterConfig;
import at.esque.kafka.cluster.SslSocketFactoryCreator;
import at.esque.kafka.controls.FilterableListView;
import at.esque.kafka.controls.JsonTreeView;
import at.esque.kafka.controls.KafkaEsqueCodeArea;
import at.esque.kafka.handlers.ConfigHandler;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.net.ssl.SSLSocketFactory;
import java.util.Collections;


public class SchemaRegistryBrowserController {

    private RestService schemaRegistryRestService;
    @FXML
    private FilterableListView<String> subjectListView;
    @FXML
    private KafkaEsqueCodeArea schemaTextArea;
    @FXML
    private ComboBox<Integer> versionComboBox;
    @FXML
    private JsonTreeView jsonTreeView;


    public void setup(ClusterConfig selectedConfig, ConfigHandler configHandler) {
        schemaRegistryRestService = new RestService(selectedConfig.getSchemaRegistry());

        if(selectedConfig.isSchemaRegistryHttps()) {
            SSLSocketFactory sslSocketFactory = SslSocketFactoryCreator.buildSSlSocketFactory(selectedConfig);
            schemaRegistryRestService.setSslSocketFactory(sslSocketFactory);
        }

        schemaRegistryRestService.configure(configHandler.getSchemaRegistryAuthProperties(selectedConfig));

        jsonTreeView.jsonStringProperty().bind(schemaTextArea.textProperty());
        try {
            versionComboBox.getSelectionModel().selectedItemProperty().addListener(((observable1, oldValue1, newValue1) -> {
                if (newValue1 == null) {
                    schemaTextArea.setText(null);
                    return;
                }
                try {
                    schemaTextArea.setText(JsonUtils.formatJson(schemaRegistryRestService.getVersion(subjectListView.getListView().getSelectionModel().getSelectedItem(), newValue1).getSchema()));
                } catch (Exception e) {
                    ErrorAlert.show(e);
                }
            }));

            subjectListView.getListView().setCellFactory(param -> subjectListCellFactory());
            subjectListView.setListComparator(String::compareTo);

            subjectListView.getListView().getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                try {
                    if (newValue != null) {
                        versionComboBox.setItems(FXCollections.observableArrayList(schemaRegistryRestService.getAllVersions(newValue)));
                    } else {
                        versionComboBox.setItems(FXCollections.emptyObservableList());
                    }
                    if (versionComboBox.getItems().size() > 0) {
                        versionComboBox.getSelectionModel().select(versionComboBox.getItems().size() - 1);
                    }
                } catch (Exception e) {
                    ErrorAlert.show(e);
                }
            });

            subjectListView.setItems(FXCollections.observableArrayList(schemaRegistryRestService.getAllSubjects()));

        } catch (Exception e) {
            ErrorAlert.show(e);
        }
    }

    public void deleteSchema() {
        String subject = subjectListView.getListView().getSelectionModel().getSelectedItem();
        Integer version = versionComboBox.getSelectionModel().getSelectedItem();
        if (subject != null && version != null && ConfirmationAlert.show("Delete Version", "Version [" + version + "] of subject [" + subject + "] will be deleted.", "Are you sure you want to delete this version?")) {
            try {
                schemaRegistryRestService.deleteSchemaVersion(Collections.emptyMap(), subject, "" + version);
                subjectListView.setItems(FXCollections.observableArrayList(schemaRegistryRestService.getAllSubjects()));
            } catch (Exception e) {
                ErrorAlert.show(e);
            }
        }
    }

    public void deleteSubject() {
        String subject = subjectListView.getListView().getSelectionModel().getSelectedItem();
        if (subject != null && ConfirmationAlert.show("Delete Subject", "Subject [" + subject + "] will be deleted.", "Are you sure you want to delete this subject?")) {
            try {
                schemaRegistryRestService.deleteSubject(Collections.emptyMap(), subject);
                subjectListView.setItems(FXCollections.observableArrayList(schemaRegistryRestService.getAllSubjects()));
            } catch (Exception e) {
                ErrorAlert.show(e);
            }
        }
    }


    private ListCell<String> subjectListCellFactory() {
        ListCell<String> cell = new ListCell<>();

        ContextMenu contextMenu = new ContextMenu();

        MenuItem deleteItem = new MenuItem();
        deleteItem.setGraphic(new FontIcon(FontAwesome.TRASH));
        deleteItem.textProperty().set("delete");
        deleteItem.setOnAction(event -> {
            deleteSubject();
        });
        contextMenu.getItems().addAll(deleteItem);

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


    public void addSubjectAndSchema(ActionEvent actionEvent) {
        showCreateSchemaDialog(null);
    }

    public void addSchemaClick(ActionEvent actionEvent) {
        showCreateSchemaDialog(subjectListView.getListView().getSelectionModel().getSelectedItem());
    }

    public void checkSchemaClick(ActionEvent actionEvent) {
        showSchemaCompatibilityCheckDialog(subjectListView.getListView().getSelectionModel().getSelectedItem(), versionComboBox.getSelectionModel().getSelectedItem());
    }

    private void showCreateSchemaDialog(String selectedSubject) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/createSchema.fxml"));
            Parent root1 = fxmlLoader.load();
            CreateSchemaController controller = fxmlLoader.getController();
            Stage stage = new Stage();
            controller.setup(selectedSubject, schemaRegistryRestService, stage);
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/kafkaesque.png")));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add Schema");
            stage.setScene(Main.createStyledScene(root1, -1, -1));
            stage.setOnCloseRequest(event -> controller.cleanup());
            stage.show();
        } catch (Exception e) {
            ErrorAlert.show(e);
        }
    }

    private void showSchemaCompatibilityCheckDialog(String selectedSubject, Integer selectedVersion) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/compatibilityCheckSchema.fxml"));
            Parent root1 = fxmlLoader.load();
            SchemaCompatibilityCheckController controller = fxmlLoader.getController();
            Stage stage = new Stage();
            controller.setup(selectedSubject, selectedVersion.toString(), schemaRegistryRestService, stage);
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/kafkaesque.png")));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Check Schema Compatibility");
            stage.setScene(Main.createStyledScene(root1, -1, -1));
            stage.setOnCloseRequest(event -> controller.cleanup());
            stage.show();
        } catch (Exception e) {
            ErrorAlert.show(e);
        }
    }

    public void refreshSubjects(ActionEvent actionEvent) {
        try {
            subjectListView.getListView().getSelectionModel().select(null);
            subjectListView.setItems(FXCollections.observableArrayList(schemaRegistryRestService.getAllSubjects()));
        } catch (Exception e) {
            ErrorAlert.show(e);
        }
    }
}
