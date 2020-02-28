package at.esque.kafka.controls;

import at.esque.kafka.lag.viewer.Lag;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class LagViewerCellContent extends VBox {
    @FXML
    private Label titleLabel;
    @FXML
    private Label currentOffsetToEndOffsetLabel;
    @FXML
    private Label messagesBehindLabel;
    @FXML
    private ProgressBar offsetProgressBar;

    public LagViewerCellContent() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "/fxml/controls/lagViewerCell.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        setup();
    }

    public void bindLaggingEntity(Lag lag) {
        titleLabel.textProperty().bind(lag.titleProperty());
        currentOffsetToEndOffsetLabel.textProperty().bind(Bindings.createStringBinding(() -> lag.getCurrentOffset() + " / " + lag.getEndOffset(),
                lag.currentOffsetProperty(),
                lag.endOffsetProperty())
        );
        messagesBehindLabel.textProperty().bind(Bindings.createStringBinding(() -> (lag.getEndOffset() - lag.getCurrentOffset())+" Messages behind",
                lag.currentOffsetProperty(),
                lag.endOffsetProperty())
        );
        offsetProgressBar.progressProperty().bind(Bindings.createDoubleBinding(() -> (double) lag.getCurrentOffset() / (double) lag.getEndOffset(),
                lag.currentOffsetProperty(),
                lag.endOffsetProperty())
        );
    }

    private void setup() {

    }
}
