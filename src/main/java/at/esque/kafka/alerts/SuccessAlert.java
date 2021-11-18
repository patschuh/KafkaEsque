package at.esque.kafka.alerts;

import at.esque.kafka.Main;
import javafx.scene.control.Alert;
import javafx.stage.Window;

public final class SuccessAlert {
    public static void show(String title, String header, String content) {
        show(title, header, content, null);
    }

    public static void show(String title, String header, String content, Window owner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        Main.applyStylesheet(alert.getDialogPane().getScene());
        Main.applyIcon(alert);

        if(owner != null){
            alert.initOwner(owner);
        }

        alert.showAndWait();
    }
}
