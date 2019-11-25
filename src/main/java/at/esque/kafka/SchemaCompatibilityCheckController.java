package at.esque.kafka;

import at.esque.kafka.alerts.ErrorAlert;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.avro.Schema;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SchemaCompatibilityCheckController {


    @FXML
    private TextField subjectTextField;

    @FXML
    private TextField versionTextField;

    @FXML
    private TextArea schemaTextArea;

    @FXML
    private Label resultLabel;

    private RestService restService;

    private Stage stage;

    public void addSchema(ActionEvent actionEvent) {

        Schema.Parser parser = new Schema.Parser();
        try {
            parser.parse(schemaTextArea.getText());
            if (restService.testCompatibility(schemaTextArea.getText(), subjectTextField.getText(), versionTextField.getText())) {
                resultLabel.setTextFill(Color.web("#3d7a3d"));
                resultLabel.setText("Schema is compatible");
            } else {
                resultLabel.setTextFill(Color.web("#bd362f"));
                resultLabel.setText("Schema is incompatible");
            }
        } catch (Exception e) {
            ErrorAlert.show(e);
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
            ErrorAlert.show(e);
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
}
