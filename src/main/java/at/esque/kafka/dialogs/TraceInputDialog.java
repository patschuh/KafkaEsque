package at.esque.kafka.dialogs;

import at.esque.kafka.Main;
import at.esque.kafka.topics.TraceDialogController;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.RadioButton;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class TraceInputDialog {

    public static final LinkedList<String> recentTrace = new LinkedList<>();

    public static Optional<TraceInput> show(boolean isAvroKeyType, boolean traceQuickSelectEnabled, List<Duration> durations, int recentTraceMaxEntries, ObservableList<Integer> partitions) throws IOException {
        Dialog<TraceInput> dialog = new Dialog<>();
        dialog.setResizable(true);
        Main.applyIcon(dialog);
        Main.applyStylesheet(dialog.getDialogPane().getScene());

        dialog.setTitle("Trace Messages");

        FXMLLoader fxmlLoader = new FXMLLoader(TraceInput.class.getResource("/fxml/traceDialog.fxml"));
        Parent root1 = fxmlLoader.load();
        TraceDialogController controller = fxmlLoader.getController();

        controller.setup(isAvroKeyType, traceQuickSelectEnabled, durations, recentTraceMaxEntries, partitions);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.getDialogPane().setContent(root1);

        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("primary");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                final String conditionMode = ((RadioButton) controller.conditionMode.getSelectedToggle()).getText();
                final String keyMode = ((RadioButton) controller.keyMode.getSelectedToggle()).getText();
                if (conditionMode.equals("key only")) {
                    updateRecentTrace(controller.keyTextBox.getText(), recentTraceMaxEntries);
                } else if (conditionMode.equals("value only")) {
                    updateRecentTrace(controller.valueTextBox.getText(), recentTraceMaxEntries);
                } else {
                    updateRecentTrace(controller.keyTextBox.getText(), recentTraceMaxEntries);
                    updateRecentTrace(controller.valueTextBox.getText(), recentTraceMaxEntries);
                }
                return new TraceInput(controller.keyTextBox.getText(), controller.valueTextBox.getText(), keyMode, conditionMode, controller.fastTraceToggle.isSelected(), controller.tombstoneToggle.isSelected(), controller.epochInstantPicker.getInstantValue() == null ? null : controller.epochInstantPicker.getInstantValue().toEpochMilli(), controller.specificParitionComboBox.getValue());
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private static void updateRecentTrace(String text, int recentTraceMaxEntries) {
        //resets element as the most recent entry if it already exists
        recentTrace.remove(text);
        recentTrace.add(text);
        while (recentTrace.size() > recentTraceMaxEntries) {
            recentTrace.removeFirst();
        }
    }

    public static class TraceInput {

        private String keySearch;
        private String valueSearch;
        private String keyMode;
        private String conditionMode;
        private boolean fastTrace;
        private boolean searchNull;
        private Long epoch;
        private Integer partition;

        public TraceInput(String keySearch, String valueSearch, String keyMode, String conditionMode, boolean fastTrace, boolean searchNull, Long epoch, Integer partition) {
            this.keySearch = keySearch;
            this.valueSearch = valueSearch;
            this.keyMode = keyMode;
            this.conditionMode = conditionMode;
            this.fastTrace = fastTrace;
            this.searchNull = searchNull;
            this.epoch = epoch;
            this.partition = partition;
        }

        public String getKeySearch() {
            return keySearch;
        }

        public void setKeySearch(String keySearch) {
            this.keySearch = keySearch;
        }

        public String getValueSearch() {
            return valueSearch;
        }

        public void setValueSearch(String valueSearch) {
            this.valueSearch = valueSearch;
        }

        public String getKeyMode() {
            return keyMode;
        }

        public void setKeyMode(String keyMode) {
            this.keyMode = keyMode;
        }

        public String getConditionMode() {
            return conditionMode;
        }

        public void setConditionMode(String conditionMode) {
            this.conditionMode = conditionMode;
        }

        public boolean isFastTrace() {
            return fastTrace;
        }

        public void setFastTrace(boolean fastTrace) {
            this.fastTrace = fastTrace;
        }

        public boolean isSearchNull() {
            return searchNull;
        }

        public void setSearchNull(boolean searchNull) {
            this.searchNull = searchNull;
        }

        public Long getEpoch() {
            return epoch;
        }

        public void setEpoch(Long epoch) {
            this.epoch = epoch;
        }

        public Integer getPartition() {
            return partition;
        }

        public void setPartition(Integer partition) {
            this.partition = partition;
        }
    }
}
