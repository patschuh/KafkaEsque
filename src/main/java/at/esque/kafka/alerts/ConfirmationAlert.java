package at.esque.kafka.alerts;

import at.esque.kafka.Main;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.util.Optional;

public class ConfirmationAlert {
    public static boolean show(String title, String header, String content) {
        return show(title, header, content, null);
    }

    public static boolean show(String title, String header, String content, Window owner) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        Main.applyIcon(alert);


        Main.applyStylesheet(alert.getDialogPane().getScene());

        Node okButton = alert.getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("primary");

        if (owner != null) {
            alert.initOwner(owner);
        }

        Optional<ButtonType> result = alert.showAndWait();
        return result.get() == ButtonType.OK;
    }
}
