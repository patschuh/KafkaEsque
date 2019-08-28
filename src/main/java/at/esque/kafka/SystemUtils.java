package at.esque.kafka;

import at.esque.kafka.alerts.ErrorAlert;
import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class SystemUtils {

    public static void copySelectionToClipboard(Callable<String> stringExtractor) {

        final ClipboardContent clipboardContent = new ClipboardContent();
        try {
            clipboardContent.putString(stringExtractor.call());
        } catch (Exception e) {
            ErrorAlert.show(e);
        }

        Clipboard.getSystemClipboard().setContent(clipboardContent);
    }

    public static Optional<String> showInputDialog(String defaultValue, String title, String header, String requestedInputLabel) {
        FutureTask<Optional<String>> futureTask = new FutureTask<>(() -> {
            TextInputDialog dialog = new TextInputDialog(defaultValue);
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(SystemUtils.class.getResource("/icons/kafkaesque.png").toString()));

            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(title);
            dialog.setHeaderText(header);
            dialog.setContentText(requestedInputLabel);
            Main.applyStylesheet(dialog.getDialogPane().getScene());

            return dialog.showAndWait();
        });
        Platform.runLater(futureTask);
        try {
            return futureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
