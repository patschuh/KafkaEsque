package at.esque.kafka.alerts;

import at.esque.kafka.Main;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;

import java.util.List;

public class TopicTemplateAppliedAlert {
    public static void show(List<String> created, List<String> existed) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Topic Template Applied");
        alert.setHeaderText("Result: ");
        Main.applyIcon(alert);
        Main.applyStylesheet(alert.getDialogPane().getScene());
        ListView<String> createdView = new ListView<>();
        createdView.getItems().addAll(created);
        ListView<String> existedView = new ListView<>();
        existedView.getItems().addAll(existed);

        GridPane gridPane = new GridPane();
        gridPane.add(new Label("Created"), 0, 0);
        gridPane.add(new Label("Existed"), 1, 0);
        gridPane.add(createdView, 0, 1);
        gridPane.add(existedView, 1, 1);
        gridPane.setMaxWidth(Double.MAX_VALUE);
        gridPane.setMaxHeight(Double.MAX_VALUE);

        alert.getDialogPane().setContent(gridPane);

        alert.showAndWait();
    }
}
