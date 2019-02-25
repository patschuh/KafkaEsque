package at.esque.kafka.dialogs;

import at.esque.kafka.Main;
import at.esque.kafka.cluster.ClusterConfig;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class ClusterConfigDialog {
    public static Optional<ClusterConfig> show() {
        return show(null);
    }

    public static Optional<ClusterConfig> show(ClusterConfig existingConfig) {

        // Create the custom dialog.
        Dialog<ClusterConfig> dialog = new Dialog<>();
        Main.applyIcon(dialog);
        dialog.setTitle(existingConfig != null?"Change Kafka Cluster":"Add new Kafka Cluster");
        dialog.setHeaderText("Provide an identifier and at least one bootstrapServer");

// Set the button types.
        ButtonType addClusterButtonType = new ButtonType(existingConfig != null?"Change":"Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addClusterButtonType, ButtonType.CANCEL);
        Main.applyStylesheet(dialog.getDialogPane().getScene());

// Create the identifier and bootstrapServer labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField identifier = new TextField();
        identifier.setPromptText("Identifier");
        TextField bootstrapServers = new TextField();
        bootstrapServers.setPromptText("booststrapServer1:9092, bootstrapServer2:9092");
        TextField schemaRegistryUrl = new TextField();
        schemaRegistryUrl.setPromptText("http://schema.registry:8081");

        grid.add(new Label("Identifier:"), 0, 0);
        grid.add(identifier, 1, 0);
        grid.add(new Label("Bootstrap Servers:"), 0, 1);
        grid.add(bootstrapServers, 1, 1);
        grid.add(new Label("Schema Registry URL:"), 0, 2);
        grid.add(schemaRegistryUrl, 1, 2);

        if(existingConfig != null){
            identifier.setText(existingConfig.getIdentifier());
            identifier.setDisable(true);
            bootstrapServers.setText(existingConfig.getBootStrapServers());
            schemaRegistryUrl.setText(existingConfig.getSchemaRegistry());
        }

// Enable/Disable login button depending on whether a username was entered.
        Node addClusterButton = dialog.getDialogPane().lookupButton(addClusterButtonType);
        addClusterButton.getStyleClass().add("primary");

        addClusterButton.disableProperty().bind(Bindings.or(bootstrapServers.textProperty().isEmpty(), identifier.textProperty().isEmpty()));

        dialog.getDialogPane().setContent(grid);

// Request focus on the identifier field by default.
        Platform.runLater(identifier::requestFocus);

// Convert the result to a ClusterConfig when the addCluster button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addClusterButtonType) {
                ClusterConfig clusterConfig = existingConfig!=null?existingConfig:new ClusterConfig();
                clusterConfig.setIdentifier(identifier.getText());
                clusterConfig.setBootStrapServers(bootstrapServers.getText());
                clusterConfig.setSchemaRegistry(schemaRegistryUrl.getText());
                return clusterConfig;
            }
            return null;
        });

        return dialog.showAndWait();


    }
}
