package at.esque.kafka.controls;

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
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.function.Function;

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
        messageTableView.setOnKeyPressed(generateMessageTableCopyEventHandler());
        helpIconToolTip.setText(buildToolTip());
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

    private EventHandler<? super KeyEvent> generateMessageTableCopyEventHandler() {
        Map<KeyCodeCombination, Function<KafkaMessage, String>> copyCombinations = Map.of(
                new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN), KafkaMessage::getValue,
                new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN), KafkaMessage::getKey,
                new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN), message -> Long.toString(message.getOffset()),
                new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN), message -> Integer.toString(message.getPartition()),
                new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN), KafkaMessage::getTimestamp
        );

        return SystemUtils.generateTableCopySelectedItemCopyEventHandler(messageTableView, copyCombinations);
    }

    public KafkaMessageTableView getMessageTableView() {
        return messageTableView;
    }

    private String buildToolTip() {
        return String.format("The following KeyCombinations let you copy data from the selected element in the messages table%n" +
                new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN).getDisplayText() + " - copy value%n" +
                new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN).getDisplayText() + " - copy key%n" +
                new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN).getDisplayText() + " - copy offset%n" +
                new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN).getDisplayText() + " - copy partition%n" +
                new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN).getDisplayText() + " - copy timestamp");
    }
}
