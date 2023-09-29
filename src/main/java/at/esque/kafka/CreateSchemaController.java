package at.esque.kafka;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.alerts.SuccessAlert;
import at.esque.kafka.controls.KafkaEsqueCodeArea;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CreateSchemaController {


    @FXML
    private TextField subjectTextField;
    @FXML
    private KafkaEsqueCodeArea schemaTextArea;

    private RestService restService;

    private Stage stage;

    public void addSchema(ActionEvent actionEvent) {

        try {
            restService.registerSchema(schemaTextArea.getText(), subjectTextField.getText());
            SuccessAlert.show("Success", null, "Schema added successfully!", getWindow());
        } catch (Exception e) {
            ErrorAlert.show(e, getWindow());
        }
    }

    public void loadSchemaFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Schema");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON File (*.json)", "*.json"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Any File (*)", "*"));
        File selectedFile = fileChooser.showOpenDialog(stage);
        try {
            String schemaString = new String(Files.readAllBytes(Paths.get(selectedFile.getPath())));
            schemaTextArea.setText(schemaString);
        } catch (IOException e) {
            ErrorAlert.show(e, getWindow());
        }
    }

    private Window getWindow() {
        return schemaTextArea.getScene().getWindow();
    }

    public void setup(String selectedSubject, RestService restService, Stage stage) {
        this.restService = restService;
        this.stage = stage;
        if (selectedSubject != null) {
            subjectTextField.setText(selectedSubject);
            subjectTextField.setDisable(true);
        }
    }

    public void cleanup() {
        restService = null;
        subjectTextField.setDisable(false);
        stage = null;
    }
}
