package at.esque.kafka.alerts;

import at.esque.kafka.Main;
import at.esque.kafka.controls.FilterableListView;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;

import java.util.List;

public class IncompatibleSchemaAlert {
    public static void show(List<String> messages, Window owner) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Schema Incompatible");
        alert.setHeaderText("Incompatibility Information");
        alert.setResizable(true);
        Main.applyIcon(alert);
        Main.applyStylesheet(alert.getDialogPane().getScene());
        FilterableListView<String> messageView = new FilterableListView<>();
        messageView.setAddButtonVisible(false);
        messageView.setRefreshButtonVisible(false);
        messageView.setItems(messages);

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(messageView);
        borderPane.setTop(new Label("Messages"));
        borderPane.setMaxWidth(Double.MAX_VALUE);
        borderPane.setMaxHeight(Double.MAX_VALUE);
        borderPane.setPrefSize(800, 600);


        alert.getDialogPane().setContent(borderPane);

        if (owner != null) {
            alert.initOwner(owner);
        }

        alert.show();
    }
}
