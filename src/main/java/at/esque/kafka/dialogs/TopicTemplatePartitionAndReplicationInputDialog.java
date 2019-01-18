package at.esque.kafka.dialogs;

import at.esque.kafka.Main;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.ShortStringConverter;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class TopicTemplatePartitionAndReplicationInputDialog {
    public static Optional<Pair<Integer, Short>> show() {
        // Create the custom dialog.
        Dialog<Pair<Integer, Short>> dialog = new Dialog<>();
        Main.applyIcon(dialog);
        Main.applyStylesheet(dialog.getDialogPane().getScene());
        dialog.setTitle("Topic Template Input");
        dialog.setHeaderText("input the fallback values to use if they are not given in the template");

// Set the button types.
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

// Create the identifier and bootstrapServer labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField partitions = new TextField();
        TextField replicationfacotr = new TextField();

        partitions.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        replicationfacotr.setTextFormatter(new TextFormatter<>(new ShortStringConverter()));

        grid.add(new Label("Partitions:"), 0, 0);
        grid.add(partitions, 1, 0);
        grid.add(new Label("replication-factor"), 0, 1);
        grid.add(replicationfacotr, 1, 1);

// Enable/Disable login button depending on whether a username was entered.
        Node addClusterButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        addClusterButton.setDisable(true);
        addClusterButton.getStyleClass().add("primary");

// Do some validation (using the Java 8 lambda syntax).
        partitions.textProperty().addListener((observable, oldValue, newValue) -> addClusterButton.setDisable(!(inputValid(newValue) && inputValid(replicationfacotr.getText()))));
        replicationfacotr.textProperty().addListener((observable, oldValue, newValue) -> addClusterButton.setDisable(!(inputValid(newValue) && inputValid(partitions.getText()))));

        dialog.getDialogPane().setContent(grid);


// Request focus on the identifier field by default.
        Platform.runLater(partitions::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new Pair<>(Integer.parseInt(partitions.getText()), Short.parseShort(replicationfacotr.getText()));
            }
            return null;
        });

        return dialog.showAndWait();


    }

    private static boolean inputValid(String input) {
        try {
            return !StringUtils.isEmpty(input) && Integer.parseInt(input) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
