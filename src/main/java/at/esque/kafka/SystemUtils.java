package at.esque.kafka;

import at.esque.kafka.alerts.ErrorAlert;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.util.concurrent.Callable;

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
}
