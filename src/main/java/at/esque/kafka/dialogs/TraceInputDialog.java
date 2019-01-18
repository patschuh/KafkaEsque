package at.esque.kafka.dialogs;

import at.esque.kafka.Main;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.converter.LongStringConverter;

import java.util.Optional;

public class TraceInputDialog {
    public static Optional<TraceInput> show(boolean isKeyTrace) {
        Dialog<TraceInput> dialog = new Dialog<>();
        Main.applyIcon(dialog);
        Main.applyStylesheet(dialog.getDialogPane().getScene());
        if (isKeyTrace) {
            dialog.setTitle("Trace Key");
            dialog.setHeaderText("Input Key to trace");
        } else {
            dialog.setTitle("Trace in Value");
            dialog.setHeaderText("Input regex to trace");
        }

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField key = new TextField();
        CheckBox fastTraceFlag = new CheckBox();
        TextField epochTimestampText = new TextField();
        Label startTimeLabel = new Label("start Epoch Timestamp");

        if (isKeyTrace) {
            Label fastTraceLabel = new Label("use fast trace:");
            fastTraceLabel.setTooltip(new Tooltip("Fast Trace traces in one partition determined by the default partitioning"));
            grid.add(fastTraceLabel, 0, 1);
            grid.add(fastTraceFlag, 1, 1);
            key.setPromptText("search");
            grid.add(new Label("Key:"), 0, 0);
        } else {
            key.setPromptText("regex");
            grid.add(new Label("regex:"), 0, 0);
        }
        epochTimestampText.setTextFormatter(new TextFormatter<>(new LongStringConverter()));
        grid.add(key, 1, 0);
        grid.add(startTimeLabel, 0, 2);
        grid.add(epochTimestampText, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("primary");

        Platform.runLater(key::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                String epochTime = epochTimestampText.getText();
                return new TraceInput(key.getText(), fastTraceFlag.isSelected(), epochTime.isEmpty() ? null : Long.parseLong(epochTime));
            }
            return null;
        });

        return dialog.showAndWait();
    }

    public static class TraceInput {

        private String search;
        private boolean fastTrace;
        private Long epoch;

        public TraceInput(String key, boolean fastTrace, Long epoch) {
            this.search = key;
            this.fastTrace = fastTrace;
            this.epoch = epoch;
        }

        public String getSearch() {
            return search;
        }

        public void setSearch(String search) {
            this.search = search;
        }

        public boolean isFastTrace() {
            return fastTrace;
        }

        public void setFastTrace(boolean fastTrace) {
            this.fastTrace = fastTrace;
        }

        public Long getEpoch() {
            return epoch;
        }

        public void setEpoch(Long epoch) {
            this.epoch = epoch;
        }
    }
}
