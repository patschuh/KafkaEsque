package at.esque.kafka.alerts;

import at.esque.kafka.Main;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorAlert {

    public static void show(Throwable ex){
        show(ex, null, true);
    }

    public static void show(Throwable ex, Window owner){
        show(ex, owner, true);
    }

    public static void show(Throwable ex, Window owner, boolean expandable) {
        show(ex.getClass().getSimpleName(), ex.getClass().getName(), ex.getMessage(), ex, owner, expandable);
    }

        public static void show(String title, String headerText, String content, Throwable ex, Window owner, boolean expandable) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(content);
        Main.applyIcon(alert);
        Main.applyStylesheet(alert.getDialogPane().getScene());

        if (expandable) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String exceptionText = sw.toString();

            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);

            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(textArea, 0, 0);

            // Set expandable Exception into the dialog pane.
            alert.getDialogPane().setExpandableContent(expContent);
        }

        if(owner != null){
            alert.initOwner(owner);
        }

        alert.showAndWait();
    }
}
