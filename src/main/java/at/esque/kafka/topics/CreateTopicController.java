package at.esque.kafka.topics;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.alerts.SuccessAlert;
import at.esque.kafka.cluster.KafkaesqueAdminClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.util.converter.IntegerStringConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CreateTopicController {

    private KafkaesqueAdminClient adminClient;

    @FXML
    public TextField topicName;
    @FXML
    public TextField numberOfPartitions;
    @FXML
    public TextField replicationFactor;
    @FXML
    public ListView<TopicConfig> configValueList;

    public void setup(KafkaesqueAdminClient adminClient) {
        this.adminClient = adminClient;
        numberOfPartitions.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        replicationFactor.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
    }

    @FXML
    public void createTopicClick(ActionEvent event) {
        try {
            adminClient.createTopic(topicName.getText(), Integer.parseInt(numberOfPartitions.getText()), Short.parseShort(replicationFactor.getText()), configsAsMap());
            SuccessAlert.show("Topic Created", null, "creation of topic [" + topicName.getText() + "] successful");
        } catch (Exception e) {
            ErrorAlert.show(e);
        }
    }

    private Map<String, String> configsAsMap() {
        HashMap<String, String> configMap = new HashMap<>();
        configValueList.getItems().forEach(config -> configMap.put(config.getKey(), config.getValue()));
        return configMap;
    }

    @FXML
    public void addConfig(ActionEvent event) {
        // Create the custom dialog.
        Dialog<TopicConfig> dialog = new Dialog<>();
        dialog.setTitle("Add Topic Config");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField configKey = new TextField();
        TextField configValue = new TextField();

        grid.add(new Label("Config Key:"), 0, 0);
        grid.add(configKey, 1, 0);
        grid.add(new Label("Config Value:"), 0, 1);
        grid.add(configValue, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(configKey::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return new TopicConfig(configKey.getText(), configValue.getText());
            }
            return null;
        });

        Optional<TopicConfig> result = dialog.showAndWait();
        result.ifPresent(tc -> configValueList.getItems().add(tc));
    }

    @FXML
    public void removeSelectedConfig(ActionEvent event) {
        configValueList.getItems().remove(configValueList.getSelectionModel().getSelectedIndex());
    }
}
