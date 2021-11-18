package at.esque.kafka;

import at.esque.kafka.acl.viewer.Acl;
import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.alerts.SuccessAlert;
import at.esque.kafka.alerts.WarningAlert;
import at.esque.kafka.connect.ConnectConfigParameter;
import at.esque.kafka.connect.KafkaesqueConnectClient;
import at.esque.kafka.connect.ValidationResult;
import at.esque.kafka.connect.utils.ConnectUtil;
import at.esque.kafka.controls.KafkaEsqueCodeArea;
import at.esque.kafka.dialogs.SubjectConfigDialog;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.avro.Schema;
import org.apache.commons.lang3.StringUtils;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPluginConfigValidationResults;

import javax.swing.text.TabableView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class CreateConnectorController {


    @FXML
    private ComboBox connectorClassCombo;

    @FXML
    private KafkaEsqueCodeArea connectorConfigTextArea;

    @FXML
    private TextField connectorNameField;

    private boolean newConnectorMode;

    private KafkaesqueConnectClient kafkaesqueConnectClient;

    private Stage stage;

    @FXML
    private TableView paramHelpView;

    @FXML
    TableColumn<ConnectConfigParameter, String> paramName;
    @FXML
    TableColumn<ConnectConfigParameter, String> paramDisplayName;
    @FXML
    TableColumn<ConnectConfigParameter, String> paramType;
    @FXML
    TableColumn<ConnectConfigParameter, String> paramDefaultValue;
    @FXML
    TableColumn<ConnectConfigParameter, String> paramImportance;
    @FXML
    TableColumn<ConnectConfigParameter, String> paramDocumentation;
    @FXML
    TableColumn<ConnectConfigParameter, String> paramGroup;
    @FXML
    TableColumn<ConnectConfigParameter, Boolean> paramRequired;
    @FXML
    TableColumn<ConnectConfigParameter, Integer> paramOrder;

    @FXML
    Button saveButton;

    private List<ConnectConfigParameter> currentConnectorClassParameters;

    public void setup(String selectedConnector, KafkaesqueConnectClient kafkaesqueConnectClient, Stage stage) {
        this.kafkaesqueConnectClient = kafkaesqueConnectClient;
        this.stage = stage;
        currentConnectorClassParameters = new ArrayList<>();


        paramName.setCellValueFactory(new PropertyValueFactory<>("name"));
        paramDisplayName.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        paramType.setCellValueFactory(new PropertyValueFactory<>("type"));
        paramDefaultValue.setCellValueFactory(new PropertyValueFactory<>("defaultValue"));
        paramImportance.setCellValueFactory(new PropertyValueFactory<>("importance"));
        paramDocumentation.setCellValueFactory(new PropertyValueFactory<>("documentation"));
        paramGroup.setCellValueFactory(new PropertyValueFactory<>("group"));
        paramRequired.setCellValueFactory(new PropertyValueFactory<>("required"));
        paramOrder.setCellValueFactory(new PropertyValueFactory<>("order"));

        if (selectedConnector == null) {
            newConnectorMode = true;

            setupConnectorClassCombo();

        }
        else
        {
            newConnectorMode = false;

            Map<String,String> currentConfig = kafkaesqueConnectClient.getConnectorConfig(selectedConnector);
            String connectorClass = currentConfig.get("connector.class");

            connectorConfigTextArea.setText(ConnectUtil.buildConfigString(currentConfig, ConnectUtil.PARAM_BLACK_LIST_EDIT));

            connectorClassCombo.setDisable(true);
            connectorClassCombo.setItems(FXCollections.observableArrayList(Arrays.asList(connectorClass)));
            connectorClassCombo.getSelectionModel().select(connectorClass);

            fetchAndShowPossibleConnectorConfigParams(connectorClass);

            connectorNameField.setDisable(true);
            connectorNameField.setText(selectedConnector);

            saveButton.setText("Update");

        }


    }

    public void cleanup() {
        kafkaesqueConnectClient = null;
        stage = null;
    }

    public void addConnector(ActionEvent actionEvent) {

        try {
            Map<String, String> paramMap = ConnectUtil.parseConfigMapFromJsonString(connectorConfigTextArea.getText());

            String connectorClass = (String) connectorClassCombo.getValue();
            String connectorName = connectorNameField.getText();

            if(newConnectorMode == true) {
                kafkaesqueConnectClient.createConnector(connectorName, connectorClass, paramMap);

                SuccessAlert.show("Success", null, "Connector added successfully!", getWindow());
            }
            else
            {
                kafkaesqueConnectClient.updateConnector(connectorName, connectorClass, paramMap);
                SuccessAlert.show("Success", null, "Connector updated successfully!", getWindow());
            }

            stage.close();
        } catch (Exception e) {
            ErrorAlert.show(e, getWindow());
        }

    }

    private void setupConnectorClassCombo() {
        List<String> availPlugins = kafkaesqueConnectClient.getInstalledConnectorPlugins();
        ListProperty<String> availPluginProperties = new SimpleListProperty<>(FXCollections.observableArrayList(availPlugins));
        connectorClassCombo.setItems(availPluginProperties);

        connectorClassCombo.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {

                fetchAndShowPossibleConnectorConfigParams((String) newValue);
                showInitialConnectorConfig();
            }
        });
    }

    private void fetchAndShowPossibleConnectorConfigParams(String connectorClass)
    {
        currentConnectorClassParameters.clear();
        currentConnectorClassParameters.addAll(kafkaesqueConnectClient.getConnectorConfigParameters(connectorClass));

        paramHelpView.setItems(FXCollections.observableArrayList(currentConnectorClassParameters));
    }

    private void showInitialConnectorConfig()
    {
        Map<String, String> initialMap = new HashMap<>();

        currentConnectorClassParameters.stream()
                .filter(c -> c.getImportance().equals("HIGH"))
                .forEach(c -> initialMap.put(c.getName(), c.getValue() != null ? c.getValue() : c.getDefaultValue()));

        connectorConfigTextArea.setText(ConnectUtil.buildConfigString(initialMap, ConnectUtil.PARAM_BLACK_LIST_EDIT));
    }

    public void validateConfig(ActionEvent actionEvent) {

        try {
            Map<String, String> paramMap = ConnectUtil.parseConfigMapFromJsonString(connectorConfigTextArea.getText());

            String connectorClass = (String) connectorClassCombo.getValue();
            String connectorName = connectorNameField.getText();

            ValidationResult validationResult = kafkaesqueConnectClient.validateConnectorConfig(connectorName, connectorClass, paramMap);

            if (validationResult.getErrorCount() == 0) {
                SuccessAlert.show("Success", null, "No validation error found!", getWindow());
            } else {
                StringBuilder errorProtocol = new StringBuilder();

                for (String param : validationResult.getErrors().keySet()) {
                    errorProtocol.append(String.format("%s:", param));
                    errorProtocol.append(System.getProperty("line.separator"));

                    for (String error : validationResult.getErrors().get(param)) {
                        errorProtocol.append(String.format("  %s", error));
                        errorProtocol.append(System.getProperty("line.separator"));
                    }
                }

                WarningAlert.show(String.format("%d validation errors found!", validationResult.getErrorCount()), null, errorProtocol.toString(), getWindow());
            }


        } catch (Exception e) {
            ErrorAlert.show(e, getWindow());
        }
    }

    private Window getWindow() {
        return connectorConfigTextArea.getScene().getWindow();
    }
}
