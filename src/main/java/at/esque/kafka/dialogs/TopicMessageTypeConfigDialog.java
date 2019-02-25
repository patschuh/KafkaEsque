package at.esque.kafka.dialogs;

import at.esque.kafka.Main;
import at.esque.kafka.MessageType;
import at.esque.kafka.cluster.TopicMessageTypeConfig;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class TopicMessageTypeConfigDialog {
    public static Optional<TopicMessageTypeConfig> show(TopicMessageTypeConfig existingConfig) {

        if(existingConfig == null){
            throw new IllegalArgumentException("no TopicMessageTypeConfig passed to dialog!");
        }

        Dialog<TopicMessageTypeConfig> dialog = new Dialog<>();
        Main.applyIcon(dialog);
        dialog.setTitle("Configure Message Types");
        dialog.setHeaderText("Configure the types of key and value for records contained in this topic");

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Main.applyStylesheet(dialog.getDialogPane().getScene());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<MessageType> keyType = new ComboBox<>();
        ComboBox<MessageType> valueType = new ComboBox<>();

        keyType.setItems(FXCollections.observableArrayList(MessageType.values()));
        valueType.setItems(FXCollections.observableArrayList(MessageType.values()));
        keyType.getSelectionModel().select(existingConfig.getKeyType());
        valueType.getSelectionModel().select(existingConfig.getValueType());

        grid.add(new Label("Key Type:"), 0, 0);
        grid.add(keyType, 1, 0);
        grid.add(new Label("Value Type:"), 0, 1);
        grid.add(valueType, 1, 1);


        Node addClusterButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        addClusterButton.getStyleClass().add("primary");

        addClusterButton.disableProperty().bind(Bindings.or(valueType.getSelectionModel().selectedItemProperty().isNull(), keyType.getSelectionModel().selectedItemProperty().isNull()));

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(keyType::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                existingConfig.setKeyType(keyType.getValue());
                existingConfig.setValueType(valueType.getValue());
                return existingConfig;
            }
            return null;
        });

        return dialog.showAndWait();


    }
}
