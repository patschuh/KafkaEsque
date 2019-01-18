package at.esque.kafka.dialogs;

import at.esque.kafka.Main;
import at.esque.kafka.cluster.ClusterConfig;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.util.List;
import java.util.Optional;

public class DeleteClustersDialog {
    public static Optional<List<ClusterConfig>> show(ObservableList<ClusterConfig> clusterConfigs) {
        // Create the custom dialog.
        Dialog<List<ClusterConfig>> dialog = new Dialog<>();
        Main.applyIcon(dialog);
        dialog.setTitle("Remove  Kafka Clusters");
        dialog.setHeaderText("Select the configurations you want to remove from the list bellow.");

// Set the button types.
        ButtonType addClusterButtonType = new ButtonType("Remove", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addClusterButtonType, ButtonType.CANCEL);
        Main.applyStylesheet(dialog.getDialogPane().getScene());

// Create the identifier and bootstrapServer labels and fields.

        ListView<ClusterConfig> listView = new ListView<>();
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setItems(clusterConfigs);

// Enable/Disable remove button
        Node addClusterButton = dialog.getDialogPane().lookupButton(addClusterButtonType);
        addClusterButton.setDisable(true);

        addClusterButton.getStyleClass().add("danger");

        listView.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> addClusterButton.setDisable(newValue == null)));

        dialog.getDialogPane().setContent(listView);

// return clusterconfigs to delete as result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addClusterButtonType) {
                return listView.getSelectionModel().getSelectedItems();
            }
            return null;
        });

        return dialog.showAndWait();


    }
}
