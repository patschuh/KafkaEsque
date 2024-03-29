package at.esque.kafka.topics;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.controls.InstantPicker;
import at.esque.kafka.topics.model.KafkaHeaderFilterOption;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.controlsfx.control.PopOver;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static at.esque.kafka.dialogs.TraceInputDialog.recentTrace;

public class TraceDialogController {

    public HBox quickSelectStartEpochButtonBar;
    public InstantPicker epochStartInstantPicker;
    public HBox quickSelectEndEpochButtonBar;
    public InstantPicker epochEndInstantPicker;
    public ToggleButton epochToggleButton;
    public RadioButton traceModeKeyOnlyRadio;
    public ToggleGroup conditionMode;
    public RadioButton traceModeValueRadio;
    public RadioButton traceModeAndRadio;
    public RadioButton traceModeOrRadio;
    public RadioButton keyModeRegexRadio;
    public RadioButton keyModeExactMatchRadio;
    public ToggleGroup keyMode;
    public CheckBox fastTraceToggle;
    public TextField keyTextBox;
    public Button keyHistoryButton;
    public TextField valueTextBox;
    public Button valueHistoryButton;
    public CheckBox tombstoneToggle;
    public TitledPane keyOptionsPane;
    public TitledPane valueOptionsPane;
    public ComboBox<Integer> specificParitionComboBox;
    public TableView<KafkaHeaderFilterOption> headerTableView;
    public TableColumn<KafkaHeaderFilterOption, String> headerFilterHeaderColumn;
    public TableColumn<KafkaHeaderFilterOption, String> headerFilterFilterStringColumn;
    public TableColumn<KafkaHeaderFilterOption, Boolean> headerFilterExactMatchColumn;

    @FXML
    public void initialize() {
        keyOptionsPane.disableProperty().bind(traceModeValueRadio.selectedProperty());
        valueOptionsPane.disableProperty().bind(traceModeKeyOnlyRadio.selectedProperty());
        epochStartInstantPicker.displayAsEpochProperty().bind(epochToggleButton.selectedProperty());
    }

    public void setup(boolean isAvroKeyType, boolean traceQuickSelectEnabled, List<Duration> durations, ObservableList<Integer> partitions) {
        clearButtonBar();
        if (traceQuickSelectEnabled) {
            fillButtonBar(durations, quickSelectStartEpochButtonBar, epochStartInstantPicker);
            fillButtonBar(durations, quickSelectEndEpochButtonBar, epochEndInstantPicker);
        }
        if (!isAvroKeyType) {
            fastTraceToggle.setDisable(false);
            fastTraceToggle.disableProperty().bind(Bindings.or(keyModeRegexRadio.selectedProperty(), traceModeOrRadio.selectedProperty()));
        } else {
            fastTraceToggle.setDisable(true);
        }
        specificParitionComboBox.setItems(partitions);
        specificParitionComboBox.getSelectionModel().select(Integer.valueOf(-1));
        ListView<String> recentTracesKey = buildRecentTracesView(keyTextBox);
        PopOver popOverKey = buildPopover(recentTracesKey);
        ListView<String> recentTracesValue = buildRecentTracesView(valueTextBox);
        PopOver popOverValue = buildPopover(recentTracesValue);

        keyHistoryButton.setOnAction(event -> popOverKey.show(keyHistoryButton));
        valueHistoryButton.setOnAction(event -> popOverValue.show(valueHistoryButton));

        headerFilterHeaderColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        headerFilterFilterStringColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        headerFilterExactMatchColumn.setCellFactory(CheckBoxTableCell.forTableColumn(new Callback<Integer, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(Integer param) {
                KafkaHeaderFilterOption kafkaHeaderFilterOption = headerTableView.getItems().get(param);
                SimpleBooleanProperty simpleBooleanProperty = new SimpleBooleanProperty(kafkaHeaderFilterOption.isExactMatch());
                simpleBooleanProperty.addListener((observable, oldValue, newValue) -> kafkaHeaderFilterOption.setExactMatch(newValue))
                ;
                return simpleBooleanProperty;
            }
        }, false));
        headerFilterHeaderColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getHeader()));
        headerFilterFilterStringColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilterString()));
        headerFilterExactMatchColumn.setCellValueFactory(param -> new SimpleBooleanProperty(param.getValue().isExactMatch()));
        headerFilterHeaderColumn.setOnEditCommit(event -> {
            KafkaHeaderFilterOption current = event.getTableView().getItems().get(event.getTablePosition().getRow());
            current.setHeader(event.getNewValue());
        });
        headerFilterFilterStringColumn.setOnEditCommit(event -> {
            KafkaHeaderFilterOption current = event.getTableView().getItems().get(event.getTablePosition().getRow());
            current.setFilterString(event.getNewValue());
        });
    }

    private void clearButtonBar() {
        quickSelectStartEpochButtonBar.getChildren().clear();
    }

    private void fillButtonBar(List<Duration> durations, HBox buttonBar, InstantPicker targetInstantPicker) {
        Button todayButton = new Button("Today");
        todayButton.setOnAction(event -> {
            OffsetDateTime offsetDateTime = Instant.now().atOffset(ZoneOffset.UTC);
            Instant today = OffsetDateTime.of(offsetDateTime.toLocalDate(), LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC).toInstant();
            targetInstantPicker.setInstantValue(today);
        });
        buttonBar.getChildren().add(todayButton);
        durations.forEach(duration -> {
            Button button = new Button("Now - " + stringifyDuration(duration));
            button.setOnAction(event -> {
                try {
                    targetInstantPicker.setInstantValue(Instant.now().minus(duration));
                } catch (Exception e) {
                    ErrorAlert.show(e);
                }
            });
            buttonBar.getChildren().add(button);
        });
    }

    private String stringifyDuration(Duration duration) {
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

    @FXML
    private void addHeaderClick(ActionEvent event) {
        headerTableView.getItems().add(new KafkaHeaderFilterOption("header", "regex or exact string to match", false));
    }

    @FXML
    private void removeHeaderClick(ActionEvent e) {
        int selectedIndex = headerTableView.getSelectionModel().getSelectedIndex();
        if (selectedIndex > -1) {
            headerTableView.getItems().remove(selectedIndex);
        }
    }
}
