//package at.esque.kafka.dialogs;
//
//import javafx.scene.control.Button;
//import javafx.scene.layout.StackPane;
//import javafx.stage.Stage;
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.testfx.api.FxToolkit;
//import org.testfx.framework.junit.ApplicationTest;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.concurrent.TimeoutException;
//
//import static org.testfx.assertions.api.Assertions.assertThat;
//
//public class TraceInputDialogTest extends ApplicationTest {
//
//    @Before
//    public void setup() throws TimeoutException {
//        FxToolkit.setupSceneRoot(() -> {
//            Button openDialogButton = new Button("Open Dialog");
//            openDialogButton.setId("openDialog");
//            openDialogButton.setOnAction(event -> {
//                try {
//                    TraceInputDialog.show(true, false, new ArrayList<>(), 0);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            });
//            StackPane root = new StackPane(openDialogButton);
//            root.setPrefSize(800, 500);
//            return new StackPane(root);
//        });
//        FxToolkit.setupStage(Stage::show);
//    }
//
//    @Test
//    @Ignore
//    public void shouldContainFastTraceForNonAvroKey(){
//        clickOn("#openDialog");
//        assertThat(targetWindow("Trace Message").lookup(".label").nth(2).queryLabeled().getText()).contains("fast trace");
//    }
//}
