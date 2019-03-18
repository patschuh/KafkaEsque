package at.esque.kafka.dialogs;

import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.Before;
import org.junit.Test;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;

import java.util.concurrent.TimeoutException;

import static org.testfx.assertions.api.Assertions.assertThat;

public class TraceInputDialogTest extends ApplicationTest {

    @Before
    public void setup() throws TimeoutException {
        FxToolkit.setupSceneRoot(() -> {
            Button openDialogButton = new Button("Open Dialog");
            openDialogButton.setId("openDialog");
            openDialogButton.setOnAction(event -> {
                TraceInputDialog.show(true, false);
            });
            StackPane root = new StackPane(openDialogButton);
            root.setPrefSize(800, 500);
            return new StackPane(root);
        });
        FxToolkit.setupStage(Stage::show);
    }

    @Test
    public void shouldContainFastTraceForNonAvroKey(){
        clickOn("#openDialog");
        assertThat(targetWindow("Trace Key").lookup(".label").nth(2).queryLabeled().getText()).contains("fast trace");
    }
}
