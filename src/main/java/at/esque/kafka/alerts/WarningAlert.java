package at.esque.kafka.alerts;

import at.esque.kafka.Main;
import javafx.scene.control.Alert;

public final class WarningAlert {
    public static void show(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        Main.applyStylesheet(alert.getDialogPane().getScene());
        Main.applyIcon(alert);

        alert.showAndWait();
    }
}
