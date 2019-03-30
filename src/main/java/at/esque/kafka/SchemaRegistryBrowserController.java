package at.esque.kafka;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.controls.FilterableListView;
import at.esque.kafka.controls.JsonTreeView;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import java.util.Arrays;


public class SchemaRegistryBrowserController {

    private RestService schemaRegistryRestService;
    @FXML
    private FilterableListView subjectListView;
    @FXML
    private TextArea schemaTextArea;
    @FXML
    private ComboBox<Integer> versionComboBox;
    @FXML
    private JsonTreeView jsonTreeView;

    public void setup(String schemaregistryUrl) {
        schemaRegistryRestService = new RestService(schemaregistryUrl);
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
            subjectListView.getListView().getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                try {
                    versionComboBox.setItems(FXCollections.observableArrayList(schemaRegistryRestService.getAllVersions(newValue)));
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
}
