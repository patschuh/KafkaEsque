package at.esque.kafka.controls;

import at.esque.kafka.JsonUtils;
import at.esque.kafka.SystemUtils;
import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.topics.KafkaMessage;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
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
    @FXML
    private Tooltip helpIconToolTip;

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
            messageTableView.getFilteredMessages().setPredicate(km ->
                    (StringUtils.trimToNull(newValue) == null
                            || (km.getKey() != null && StringUtils.containsIgnoreCase(km.getKey(), newValue)
                            || (km.getValue() != null && StringUtils.containsIgnoreCase(km.getValue(), newValue)))
                    ));
        });
        messageTableView.getSelectionModel().setCellSelectionEnabled(true);
        messageTableView.setOnKeyPressed(generateMessageTableCopyEventHandler());
        helpIconToolTip.setText(buildToolTip());
        helpIconToolTip.setShowDelay(Duration.seconds(1));
    }

    @FXML
    public void exportFileClick(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save messages to file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON File (*.json)", "*.json"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV File (*.csv)", "*.csv"));
        File selectedFile = fileChooser.showSaveDialog(this.getScene().getWindow());
        if (selectedFile != null) {
            String fileName = selectedFile.getName();
            String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
            try (Writer writer = new FileWriter(selectedFile.getAbsolutePath())) {
                if ("json".equals(fileExtension)) {
                    try {
                        JsonUtils.writeMessageToJsonFile(messageTableView.getItems(), writer);
                    } catch (Exception e) {
                        ErrorAlert.show(e);
                    }
                } else {
                    StatefulBeanToCsv<KafkaMessage> beanToCsv = new StatefulBeanToCsvBuilder<KafkaMessage>(writer).build();
                    messageTableView.getItems().forEach(message -> {
                        try {
                            beanToCsv.write(message);
                        } catch (Exception e) {
                            ErrorAlert.show(e);
                        }
                    });
                }
            } catch (Exception e) {
                ErrorAlert.show(e);
            }
        }
    }

    private EventHandler<? super KeyEvent> generateMessageTableCopyEventHandler() {
        return SystemUtils.generateTableCopySelectedCellsCopyEventHandler(messageTableView);
    }

    public KafkaMessageTableView getMessageTableView() {
        return messageTableView;
    }

    private String buildToolTip() {
        return String.format("Select one or more table cells (Shift/Ctrl-click or range selection) and press %s to copy. " +
                        "Cells are copied row-wise with tab separation and line breaks between rows.%n%n" +
                        "Double-click a cell to make a line selection.",
                new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN).getDisplayText());
    }
}
