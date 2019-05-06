package at.esque.kafka.handlers;

import at.esque.kafka.alerts.ErrorAlert;
import com.google.inject.Singleton;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.*;

@Singleton
public class TrayHandler {

    private final boolean traySupported;
    private final TrayIcon trayIcon;
    private final SystemTray systemTray;
    private Stage primaryStage;

    public TrayHandler() {
        traySupported = SystemTray.isSupported();
        if (traySupported) {

            systemTray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage(getClass().getResource("/icons/trayIcon.png"));
            trayIcon = new TrayIcon(image, "KafkaEsque");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("KafkaEsque");

            PopupMenu popupMenu = new PopupMenu();
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                Platform.exit();
            });

            MenuItem showStageItem = new MenuItem("Open");
            showStageItem.addActionListener(e -> {
                Platform.runLater(() -> primaryStage.show());
            });

            MenuItem hideStageItem = new MenuItem("Hide");
            hideStageItem.addActionListener(e -> {
                Platform.runLater(() -> primaryStage.hide());
            });

            popupMenu.add(exitItem);
            popupMenu.add(showStageItem);
            popupMenu.add(hideStageItem);
            try {
                trayIcon.setPopupMenu(popupMenu);
                systemTray.add(trayIcon);
                Platform.setImplicitExit(false);
            } catch (AWTException e) {
                ErrorAlert.show(e);
            }
        } else {
            systemTray = null;
            trayIcon = null;
        }
    }

    public void showInfoNotification(String title, String message) {
        if (traySupported) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    public void showWarningNotification(String title, String message) {
        if (traySupported) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.WARNING);
        }
    }

    public void showErrorNotification(String title, String message) {
        if (traySupported) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.ERROR);
        }
    }

    public void shutdown() {
        System.exit(0);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public boolean isTraySupported() {
        return traySupported;
    }
}
