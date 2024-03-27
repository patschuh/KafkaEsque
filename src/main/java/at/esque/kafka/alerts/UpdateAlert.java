package at.esque.kafka.alerts;

import at.esque.kafka.Main;
import at.esque.kafka.alerts.model.UpdateDialogResult;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.util.Optional;

public class UpdateAlert {
    public static UpdateDialogResult show(String title, String header, String content) {
        return show(title, header, content, null);
    }

    public static UpdateDialogResult show(String title, String header, String content, Window owner) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        Main.applyIcon(alert);

        ButtonType open = new ButtonType("Open", ButtonBar.ButtonData.FINISH);
        ButtonType askAgain = new ButtonType("Ask again in 24h", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(open, askAgain, cancel);


        Main.applyStylesheet(alert.getDialogPane().getScene());

        Node okButton = alert.getDialogPane().lookupButton(open);
        okButton.getStyleClass().add("primary");

        if (owner != null) {
            alert.initOwner(owner);
        }

        Optional<ButtonType> result = alert.showAndWait();
        return switch (result.get().getButtonData()) {
            case FINISH -> UpdateDialogResult.OPEN;
            case OK_DONE -> UpdateDialogResult.REMIND_LATER;
            case CANCEL_CLOSE -> UpdateDialogResult.CANCEL;
            default -> UpdateDialogResult.CANCEL;
        };
    }
}
