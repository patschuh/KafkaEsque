package at.esque.kafka.dialogs;

import at.esque.kafka.Main;
import at.esque.kafka.cluster.ClusterConfig;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class AddClusterDialog {
    public static Optional<ClusterConfig> show(){
        // Create the custom dialog.
        Dialog<ClusterConfig> dialog = new Dialog<>();
        Main.applyIcon(dialog);
        dialog.setTitle("Add new Kafka Cluster");
        dialog.setHeaderText("Provide an identifier and at least one bootstrapServer");

// Set the button types.
        ButtonType addClusterButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addClusterButtonType, ButtonType.CANCEL);
        Main.applyStylesheet(dialog.getDialogPane().getScene());

// Create the identifier and bootstrapServer labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField Identifier = new TextField();
        Identifier.setPromptText("Identifier");
        TextField bootstrapServers = new TextField();
        bootstrapServers.setPromptText("booststrapServer1:9092, bootstrapServer2:9092");

        grid.add(new Label("Identifier:"), 0, 0);
        grid.add(Identifier, 1, 0);
        grid.add(new Label("Bootstrap Servers:"), 0, 1);
        grid.add(bootstrapServers, 1, 1);

// Enable/Disable login button depending on whether a username was entered.
        Node addClusterButton = dialog.getDialogPane().lookupButton(addClusterButtonType);
        addClusterButton.setDisable(true);
        addClusterButton.getStyleClass().add("primary");

// Do some validation (using the Java 8 lambda syntax).
        Identifier.textProperty().addListener((observable, oldValue, newValue) -> addClusterButton.setDisable(newValue.trim().isEmpty()||bootstrapServers.getText().isEmpty()));

        bootstrapServers.textProperty().addListener((observable, oldValue, newValue) -> addClusterButton.setDisable(newValue.trim().isEmpty()||Identifier.getText().isEmpty()));

        dialog.getDialogPane().setContent(grid);

// Request focus on the identifier field by default.
        Platform.runLater(Identifier::requestFocus);

// Convert the result to a ClusterConfig when the addCluster button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addClusterButtonType) {
                ClusterConfig clusterConfig = new ClusterConfig();
                clusterConfig.setIdentifier(Identifier.getText());
                clusterConfig.setBootStrapServers(bootstrapServers.getText());
                return clusterConfig;
            }
            return null;
        });

        return dialog.showAndWait();


    }
}
