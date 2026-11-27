package at.esque.kafka.controls;

import at.esque.kafka.topics.KafkaMessage;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;

public class KafkaMessageTableView extends TableView<KafkaMessage> {
    private ObservableList<KafkaMessage> baseList;
    private FilteredList<KafkaMessage> filteredMessages;
    private SortedList<KafkaMessage> sortedMessages;
    private TablePosition<KafkaMessage, ?> selectionAnchor;
    private TablePosition<KafkaMessage, ?> dragStartCell;
    private boolean draggingSelection;

    public KafkaMessageTableView() {
        this(FXCollections.observableArrayList());
    }

    public KafkaMessageTableView(ObservableList<KafkaMessage> baseList) {
        super();
        this.baseList = baseList;
        filteredMessages = new FilteredList<>(baseList, km -> true);
        sortedMessages = new SortedList<>(filteredMessages);
        sortedMessages.comparatorProperty().bind(this.comparatorProperty());
        buildTableColumns();
        this.setItems(sortedMessages);
        this.setMinHeight(0);
        this.setMinWidth(0);
        this.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                draggingSelection = false;
                dragStartCell = null;
            }
        });
    }

    private void buildTableColumns() {
        TableColumn<KafkaMessage, Long> messageOffsetColumn = new TableColumn<>("Offset");
        messageOffsetColumn.setCellValueFactory(new PropertyValueFactory<>("offset"));
        messageOffsetColumn.setPrefWidth(50);
        messageOffsetColumn.setCellFactory(param -> createDefaultSelectableCell());

        TableColumn<KafkaMessage, Integer> messagePartitionColumn = new TableColumn<>("Partition");
        messagePartitionColumn.setCellValueFactory(new PropertyValueFactory<>("partition"));
        messagePartitionColumn.setPrefWidth(60);
        messagePartitionColumn.setCellFactory(param -> createDefaultSelectableCell());

        TableColumn<KafkaMessage, String> messageTimestampColumn = new TableColumn<>("Timestamp");
        messageTimestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        messageTimestampColumn.setPrefWidth(150);
        messageTimestampColumn.setCellFactory(param -> createDefaultSelectableCell());

        TableColumn<KafkaMessage, String> messageKeyColumn = new TableColumn<>("Key");
        messageKeyColumn.setPrefWidth(200);
        messageKeyColumn.setCellValueFactory(param -> {
            if (param.getValue() != null && param.getValue().getKey() != null) {
                return new SimpleStringProperty(param.getValue().getKey().replaceAll("\\r\\n|\\r|\\n", " "));
            } else {
                return null;
            }
        });

        messageKeyColumn.setCellFactory(param -> getNullMarkingTableCell("NULL", Color.GRAY));

        TableColumn<KafkaMessage, String> messageValueColumn = new TableColumn<>("Value");
        messageValueColumn.setPrefWidth(600);
        messageValueColumn.setCellValueFactory(param -> {
            if (param.getValue() != null && param.getValue().getValue() != null) {
                return new SimpleStringProperty(param.getValue().getValue().replaceAll("\\r\\n|\\r|\\n", " "));
            } else {
                return null;
            }
        });
        messageValueColumn.setCellFactory(param -> getNullMarkingTableCell("NULL/Tombstone", Color.GRAY));
        this.getColumns().addAll(messageTimestampColumn, messagePartitionColumn, messageOffsetColumn, messageKeyColumn, messageValueColumn);
    }

    @NotNull
    private TableCell<KafkaMessage, String> getNullMarkingTableCell(String nullDisplayString, Color nullDisplayColor) {
        TableCell<KafkaMessage, String> tableCell = new TableCell<KafkaMessage, String>() {
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null && !empty) {
                    setTextFill(nullDisplayColor);
                    setText(nullDisplayString);
                } else {
                    setTextFill(Color.BLACK);
                    setText(item);
                }
            }
        };
        installCellRangeSelectionHandlers(tableCell);
        return tableCell;
    }

    @NotNull
    private <T> TableCell<KafkaMessage, T> createDefaultSelectableCell() {
        TableCell<KafkaMessage, T> tableCell = new TableCell<KafkaMessage, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                setTextFill(Color.BLACK);
            }
        };
        installCellRangeSelectionHandlers(tableCell);
        return tableCell;
    }

    private void installCellRangeSelectionHandlers(TableCell<KafkaMessage, ?> tableCell) {
        tableCell.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 2 || tableCell.isEmpty()) {
                return;
            }
            boolean keepCurrentSelection = event.isControlDown() || event.isShortcutDown();
            selectWholeRow(tableCell.getIndex(), keepCurrentSelection);
            selectionAnchor = new TablePosition<>(this, tableCell.getIndex(), tableCell.getTableColumn());
            draggingSelection = false;
            dragStartCell = null;
            event.consume();
        });

        tableCell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || tableCell.isEmpty()) {
                return;
            }
            if (event.getClickCount() > 1) {
                return;
            }

            requestFocus();
            TablePosition<KafkaMessage, ?> cellPosition = new TablePosition<>(this, tableCell.getIndex(), tableCell.getTableColumn());
            dragStartCell = cellPosition;
            draggingSelection = true;

            if (event.isShiftDown() && selectionAnchor != null && selectionAnchor.getTableColumn() != null) {
                selectCellRectangle(selectionAnchor, cellPosition, event.isControlDown() || event.isShortcutDown());
            } else if (event.isControlDown() || event.isShortcutDown()) {
                if (getSelectionModel().isSelected(cellPosition.getRow(), cellPosition.getTableColumn())) {
                    getSelectionModel().clearSelection(cellPosition.getRow(), cellPosition.getTableColumn());
                } else {
                    getSelectionModel().select(cellPosition.getRow(), cellPosition.getTableColumn());
                }
                selectionAnchor = cellPosition;
            } else {
                getSelectionModel().clearSelection();
                getSelectionModel().select(cellPosition.getRow(), cellPosition.getTableColumn());
                selectionAnchor = cellPosition;
            }

            event.consume();
        });

        tableCell.addEventFilter(MouseEvent.DRAG_DETECTED, event -> {
            if (draggingSelection && dragStartCell != null && !tableCell.isEmpty()) {
                tableCell.startFullDrag();
                event.consume();
            }
        });

        tableCell.addEventFilter(MouseDragEvent.MOUSE_DRAG_ENTERED, event -> {
            if (!draggingSelection || dragStartCell == null || tableCell.isEmpty() || !event.isPrimaryButtonDown()) {
                return;
            }
            TablePosition<KafkaMessage, ?> cellPosition = new TablePosition<>(this, tableCell.getIndex(), tableCell.getTableColumn());
            selectCellRectangle(dragStartCell, cellPosition, false);
            event.consume();
        });
    }

    public ObservableList<KafkaMessage> getBaseList() {
        return baseList;
    }

    public FilteredList<KafkaMessage> getFilteredMessages() {
        return filteredMessages;
    }

    public SortedList<KafkaMessage> getSortedMessages() {
        return sortedMessages;
    }

    private void selectCellRectangle(TablePosition<KafkaMessage, ?> firstCell, TablePosition<KafkaMessage, ?> secondCell, boolean keepCurrentSelection) {
        int firstColumnIndex = getVisibleLeafIndex(firstCell.getTableColumn());
        int secondColumnIndex = getVisibleLeafIndex(secondCell.getTableColumn());
        if (firstColumnIndex < 0 || secondColumnIndex < 0) {
            return;
        }
        int minRow = Math.min(firstCell.getRow(), secondCell.getRow());
        int maxRow = Math.max(firstCell.getRow(), secondCell.getRow());
        int minColumn = Math.min(firstColumnIndex, secondColumnIndex);
        int maxColumn = Math.max(firstColumnIndex, secondColumnIndex);

        if (!keepCurrentSelection) {
            getSelectionModel().clearSelection();
        }

        for (int row = minRow; row <= maxRow; row++) {
            for (int column = minColumn; column <= maxColumn; column++) {
                getSelectionModel().select(row, getVisibleLeafColumn(column));
            }
        }
    }

    private void selectWholeRow(int row, boolean keepCurrentSelection) {
        if (row < 0 || row >= getItems().size()) {
            return;
        }
        if (!keepCurrentSelection) {
            getSelectionModel().clearSelection();
        }
        for (int column = 0; column < getVisibleLeafColumns().size(); column++) {
            getSelectionModel().select(row, getVisibleLeafColumn(column));
        }
    }
}
