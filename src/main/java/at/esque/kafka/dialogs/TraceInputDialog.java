package at.esque.kafka.dialogs;

import at.esque.kafka.Main;
import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.controls.InstantPicker;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.controlsfx.control.PopOver;
import org.jetbrains.annotations.NotNull;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class TraceInputDialog {

    public static final LinkedList<String> recentTrace = new LinkedList<>();

    public static Optional<TraceInput> show(boolean isKeyTrace, boolean isAvroKeyType, boolean traceQuickSelectEnabled, List<Duration> durations, int recentTraceMaxEntries) {
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
        grid.setPadding(new Insets(20, 20, 20, 20));


        TextField key = new TextField();
        key.getStyleClass().add("first");
        HBox.setHgrow(key, Priority.ALWAYS);
        HBox keyBox = buildKeyHBox(key);
        key.setPrefWidth(500);
        CheckBox fastTraceFlag = new CheckBox();
        CheckBox searchNullFlag = new CheckBox();
        HBox hBox = new HBox();
        InstantPicker instantPicker = new InstantPicker();
        instantPicker.setInstantValue(null);
        instantPicker.setMaxWidth(Double.MAX_VALUE);
        ToggleButton displayEpochMillis = new ToggleButton();
        displayEpochMillis.setGraphic(new FontIcon("fa-exchange"));
        displayEpochMillis.setMinHeight(30);
        instantPicker.displayAsEpochProperty().bind(displayEpochMillis.selectedProperty());
        HBox.setHgrow(instantPicker, Priority.ALWAYS);
        hBox.getChildren().addAll(instantPicker, displayEpochMillis);
        Label startTimeLabel = new Label("Start Epoch Timestamp");

        if (isKeyTrace) {
            if (!isAvroKeyType) {
                Label fastTraceLabel = new Label("Use fast trace:");
                fastTraceLabel.setTooltip(new Tooltip("Fast Trace traces in one partition determined by the default partitioning"));
                grid.add(fastTraceLabel, 0, 1);
                grid.add(fastTraceFlag, 1, 1);
                fastTraceFlag.setSelected(true);
            }
            key.setPromptText("search");
            grid.add(new Label("Key:"), 0, 0);
        } else {
            Label searchNullLabel = new Label("Search for null/tombstone:");
            grid.add(searchNullLabel, 0, 1);
            grid.add(searchNullFlag, 1, 1);
            key.setPromptText("regex");
            key.disableProperty().bind(searchNullFlag.selectedProperty());
            grid.add(new Label("regex:"), 0, 0);
        }
        grid.add(keyBox, 1, 0);
        grid.add(startTimeLabel, 0, 2);
        grid.add(hBox, 1, 2);

        if (traceQuickSelectEnabled) {
            HBox buttonBar = new HBox();
            fillButtonBar(buttonBar, instantPicker, durations);
            buttonBar.setMaxWidth(Double.MAX_VALUE);
            buttonBar.setAlignment(Pos.CENTER);
            HBox.setHgrow(buttonBar, Priority.ALWAYS);
            grid.add(buttonBar, 1, 3);
        }

        dialog.getDialogPane().setContent(grid);

        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("primary");

        Platform.runLater(key::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                updateRecentTrace(key.getText(), recentTraceMaxEntries);
                return new TraceInput(key.getText(), fastTraceFlag.isSelected(), searchNullFlag.isSelected(), instantPicker.getInstantValue() == null ? null : instantPicker.getInstantValue().toEpochMilli());
            }
            return null;
        });

        return dialog.showAndWait();
    }

    @NotNull
    private static HBox buildKeyHBox(TextField key) {
        HBox keyBox = new HBox();
        keyBox.getChildren().add(key);
        FontIcon icon = new FontIcon();
        icon.setIconCode(FontAwesome.HISTORY);
        ListView<String> recentTraces = buildRecentTracesView(key);
        PopOver popOver = buildPopover(recentTraces);
        Button traceHistoryButton = new Button();
        traceHistoryButton.setGraphic(icon);
        traceHistoryButton.getStyleClass().add("last");
        keyBox.getChildren().add(traceHistoryButton);
        traceHistoryButton.setOnAction(event -> {
            popOver.show(traceHistoryButton);
        });
        return keyBox;
    }

    @NotNull
    private static PopOver buildPopover(ListView<String> recentTraces) {
        PopOver popOver = new PopOver();
        popOver.setContentNode(recentTraces);
        popOver.setTitle("Recent Traces");
        popOver.setCloseButtonEnabled(true);
        popOver.setHeaderAlwaysVisible(true);
        return popOver;
    }

    @NotNull
    private static ListView<String> buildRecentTracesView(TextField key) {
        ListView<String> recentTraces = new ListView<>();
        recentTraces.setFocusTraversable(false);
        recentTraces.setMaxHeight(200);
        recentTraces.setBackground(Background.EMPTY);
        recentTraces.setCellFactory(param -> {
            ListCell<String> cell = new ListCell<>();

            cell.textProperty().bind(cell.itemProperty());

            cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                if (isNowEmpty) {
                    cell.setOnMouseClicked(null);
                } else {
                    cell.setOnMouseClicked(event -> key.setText(cell.getText()));
                }
            });

            return cell;
        });
        ArrayList<String> sortable = new ArrayList<>(recentTrace);
        Collections.reverse(sortable);
        recentTraces.setItems(FXCollections.observableArrayList(sortable));
        return recentTraces;
    }

    private static void updateRecentTrace(String text, int recentTraceMaxEntries) {
        //resets element as the most recent entry if it already exists
        recentTrace.remove(text);
        recentTrace.add(text);
        while (recentTrace.size() > recentTraceMaxEntries) {
            recentTrace.removeFirst();
        }
    }

    private static void fillButtonBar(HBox buttonBar, InstantPicker instantPicker, List<Duration> durations) {
        Button todayButton = new Button("Today");
        todayButton.setOnAction(event -> {
            OffsetDateTime offsetDateTime = Instant.now().atOffset(ZoneOffset.UTC);
            Instant today = OffsetDateTime.of(offsetDateTime.toLocalDate(), LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC).toInstant();
            instantPicker.setInstantValue(today);
        });
        buttonBar.getChildren().add(todayButton);
        durations.forEach(duration -> {
            Period period = Period.ofDays((int) duration.toDays());
            Button button = new Button("Now - " + stringifyDuration(duration));
            button.setOnAction(event -> {
                try {
                    instantPicker.setInstantValue(Instant.now().minus(duration));
                } catch (Exception e) {
                    ErrorAlert.show(e);
                }
            });
            buttonBar.getChildren().add(button);
        });
    }

    public static class TraceInput {

        private String search;
        private boolean fastTrace;
        private boolean searchNull;
        private Long epoch;

        public TraceInput(String key, boolean fastTrace, boolean searchNull, Long epoch) {
            this.search = key;
            this.fastTrace = fastTrace;
            this.searchNull = searchNull;
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
    }

    private static String stringifyDuration(Duration duration) {
        StringBuilder builder = new StringBuilder("");
        long days = duration.toDays();
        long hours = duration.minusDays(days).toHours();
        long minutes = duration.minusDays(days).minusHours(hours).toMinutes();
        long seconds = duration.minusDays(days).minusHours(hours).minusMinutes(minutes).getSeconds();
        if (days > 0) {
            builder.append(days)
                    .append(" days ");
        }
        if (hours > 0) {
            builder.append(hours)
                    .append(" hours ");
        }
        if (minutes > 0) {
            builder.append(minutes)
                    .append(" minutes ");
        }
        if (seconds > 0) {
            builder.append(seconds)
                    .append(" seconds ");
        }
        return builder.toString().trim();
    }
}
