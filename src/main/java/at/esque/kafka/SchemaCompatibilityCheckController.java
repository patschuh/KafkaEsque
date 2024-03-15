package at.esque.kafka;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.alerts.IncompatibleSchemaAlert;
import at.esque.kafka.controls.KafkaEsqueCodeArea;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.client.rest.entities.Schema;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class SchemaCompatibilityCheckController {

    @FXML
    private TextField subjectTextField;

    @FXML
    private TextField versionTextField;

    @FXML
    private KafkaEsqueCodeArea schemaTextArea;

    @FXML
    private Label resultLabel;

    private RestService restService;

    private Stage stage;

    public void addSchema(ActionEvent actionEvent) {

        try {
            Schema schema = restService.getVersion(subjectTextField.getText(), Integer.parseInt(versionTextField.getText()));
            List<String> compatibility = restService.testCompatibility(schemaTextArea.getText(), schema.getSchemaType(), null, subjectTextField.getText(), versionTextField.getText(), true);

            if (compatibility.isEmpty()) {
                resultLabel.setTextFill(Color.web("#3d7a3d"));
                resultLabel.setText("Schema is compatible");
            } else {
                resultLabel.setTextFill(Color.web("#bd362f"));
                resultLabel.setText("Schema is incompatible");
                resultLabel.setGraphic(FontIcon.of(FontAwesome.QUESTION_CIRCLE, Color.CADETBLUE));
                resultLabel.setOnMouseClicked(event -> IncompatibleSchemaAlert.show(compatibility, getWindow()));
                IncompatibleSchemaAlert.show(compatibility, getWindow());
            }
        } catch (Exception e) {
            ErrorAlert.show("Exception when requesting compatibility check", "Could not check compatibility", e.getClass().getName(), e, getWindow(), true);
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

    public void setup(String selectedSubject, String selectedVersion, RestService restService, Stage stage) {
        this.restService = restService;
        this.stage = stage;
        subjectTextField.setText(selectedSubject);
        subjectTextField.setDisable(true);
        versionTextField.setText(selectedVersion);
        versionTextField.setDisable(true);
        resultLabel.setText("");
    }

    public void cleanup() {
        restService = null;
        subjectTextField.setDisable(false);
        stage = null;
    }

    private Window getWindow() {
        return schemaTextArea.getScene().getWindow();
    }
}
