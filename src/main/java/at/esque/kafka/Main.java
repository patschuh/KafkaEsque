package at.esque.kafka;

import at.esque.kafka.guice.GuiceEsqueModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Injector injector = Guice.createInjector(new GuiceEsqueModule());
        FXMLLoader loader = injector.getInstance(FXMLLoader.class);
        loader.setLocation(getClass().getResource("/fxml/mainScene.fxml"));
        Parent root =  loader.load();
        Controller controller = loader.getController();
        controller.setup(primaryStage);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/kafkaesque.png")));
        primaryStage.setTitle("Kafkaesque");
        primaryStage.setScene(createStyledScene(root, 1600, 900));
        primaryStage.show();
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
}
