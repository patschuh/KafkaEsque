package at.esque.kafka;

import at.esque.kafka.guice.GuiceEsqueModule;
import at.esque.kafka.handlers.TrayHandler;
import com.google.inject.Guice;
import com.google.inject.Injector;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.Arrays;
import java.util.Optional;

public class Main extends Application {

    private TrayHandler trayHandler;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Injector injector = Guice.createInjector(new GuiceEsqueModule());
        trayHandler = injector.getInstance(TrayHandler.class);
        trayHandler.setPrimaryStage(primaryStage);
        FXMLLoader loader = injector.getInstance(FXMLLoader.class);
        loader.setLocation(getClass().getResource("/fxml/mainScene.fxml"));
        Parent root = loader.load();
        Controller controller = loader.getController();
        controller.setup(primaryStage);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/kafkaesque.png")));
        primaryStage.setTitle("Kafkaesque");
        primaryStage.setScene(createStyledScene(root, 1600, 900));
        primaryStage.getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::primaryStageClose);
        primaryStage.show();
    }

    private <T extends Event> void primaryStageClose(T event) {
        if (trayHandler.isTraySupported()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText("Should KafkaEsque stay open in the Tray?");

            alert.getButtonTypes().setAll(Arrays.asList(ButtonType.YES, ButtonType.NO));

            Optional<ButtonType> result = alert.showAndWait();

            if (result.orElse(ButtonType.NO) == ButtonType.YES) {
                trayHandler.getPrimaryStage().hide();
            } else {
                Platform.exit();
            }
        }
    }

    public static Scene createStyledScene(Parent parent, double width, double height) {
        Scene scene = new Scene(parent, width, height);
        applyStylesheet(scene);
        return scene;
    }

    public static void applyStylesheet(Scene scene) {
        scene.getStylesheets().add(Main.class.getResource("/css/bootstrap2.css").toExternalForm());
    }

    public static void applyIcon(Dialog dialog) {
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(Main.class.getResource("/icons/kafkaesque.png").toString()));
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() {
        trayHandler.shutdown();
    }
}
