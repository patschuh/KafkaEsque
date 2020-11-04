package at.esque.kafka.controls;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.topics.KafkaMessage;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class MessagesTabContent extends VBox {

    @FXML
    private TextField messageSearchTextField;
    @FXML
    private KafkaMessageTableView messageTableView;

    public MessagesTabContent() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "/fxml/controls/messagesTab.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.setClassLoader(getClass().getClassLoader());
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        setup();
    }

    private void setup() {
        messageSearchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            messageTableView.getFilteredMessages().setPredicate(km -> (km.getKey() != null && StringUtils.containsIgnoreCase(km.getKey(), newValue) || (km.getValue() != null && StringUtils.containsIgnoreCase(km.getValue(), newValue))));
        });
    }

    @FXML
    public void exportCsvClick(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save messages as csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV File (*.csv)", "*.csv"));
        File selectedFile = fileChooser.showSaveDialog(this.getScene().getWindow());
        if (selectedFile != null) {
            try (Writer writer = new FileWriter(selectedFile.getAbsolutePath())) {
                StatefulBeanToCsv<KafkaMessage> beanToCsv = new StatefulBeanToCsvBuilder<KafkaMessage>(writer).build();
                messageTableView.getItems().forEach(message -> {
                    try {
                        beanToCsv.write(message);
                    } catch (Exception e) {
                        ErrorAlert.show(e);
                    }
                });
            } catch (Exception e) {
                ErrorAlert.show(e);
            }
        }
    }

    public KafkaMessageTableView getMessageTableView(){
        return messageTableView;
    }
}
