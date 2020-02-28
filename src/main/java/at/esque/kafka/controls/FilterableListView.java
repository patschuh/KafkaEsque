package at.esque.kafka.controls;

import at.esque.kafka.SystemUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;

public class FilterableListView<T> extends VBox {
    @FXML
    private TextField filtertextField;
    @FXML
    private ListView<T> listView;
    @FXML
    private HBox toolbarBox;
    @FXML
    private Button addButton;
    @FXML
    private Button refreshButton;

    private ObservableList<T> baseList;
    private FilteredList<T> filteredList;
    private SortedList<T> sortedList;

    public BooleanProperty addButtonVisible = new SimpleBooleanProperty(true);
    public BooleanProperty refreshButtonVisible = new SimpleBooleanProperty(true);
    private Function<T, String> stringifierFunction = String::valueOf;

    public FilterableListView() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "/fxml/controls/filterableListView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        setup();
    }

    private void setup() {
        baseList = FXCollections.observableArrayList();
        filteredList = new FilteredList<>(baseList);
        sortedList = new SortedList<>(filteredList);
        listView.setItems(sortedList);

        filtertextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (StringUtils.isEmpty(newValue)) {
                filteredList.setPredicate(i -> true);
            } else {
                filteredList.setPredicate(item -> {
                    String stringifiedItem = getSelectedItemStringified(item);

                    return StringUtils.containsIgnoreCase(stringifiedItem, newValue);
                });
            }
        });
        listView.setOnKeyPressed(generateListEventHandler());

        bindButtonProperties();
    }

    private void bindButtonProperties() {
        addButton.onActionProperty().bind(onAddActionProperty());
        addButton.visibleProperty().bind(addButtonVisibleProperty());
        refreshButton.visibleProperty().bind(refreshButtonVisibleProperty());
        addButton.maxWidthProperty().bind(Bindings.when(addButtonVisibleProperty()).then(Region.USE_COMPUTED_SIZE).otherwise(0));
        addButton.minWidthProperty().bind(Bindings.when(addButtonVisibleProperty()).then(Region.USE_COMPUTED_SIZE).otherwise(0));
        refreshButton.maxWidthProperty().bind(Bindings.when(refreshButtonVisibleProperty()).then(Region.USE_COMPUTED_SIZE).otherwise(0));
        refreshButton.minWidthProperty().bind(Bindings.when(refreshButtonVisibleProperty()).then(Region.USE_COMPUTED_SIZE).otherwise(0));
    }

    public ObservableList<T> getBaseList() {
        return baseList;
    }

    public void setItems(Collection<T> items) {
        baseList.clear();
        baseList.addAll(items);
    }

    public void addItems(Collection<T> items) {
        baseList.addAll(items);
    }

    public ListView<T> getListView() {
        return listView;
    }

    public boolean isAddButtonVisible() {
        return addButtonVisible.get();
    }

    public BooleanProperty addButtonVisibleProperty() {
        return addButtonVisible;
    }

    public void setAddButtonVisible(boolean addButtonVisible) {
        this.addButtonVisible.set(addButtonVisible);
    }

    public boolean isRefreshButtonVisible() {
        return refreshButtonVisible.get();
    }

    public BooleanProperty refreshButtonVisibleProperty() {
        return refreshButtonVisible;
    }

    public void setRefreshButtonVisible(boolean refreshButtonVisible) {
        this.refreshButtonVisible.set(refreshButtonVisible);
    }

    public final ObjectProperty<EventHandler<ActionEvent>> onAddActionProperty() {
        return onAddAction;
    }

    public final void setOnAddAction(EventHandler<ActionEvent> value) {
        onAddActionProperty().set(value);
    }

    public final EventHandler<ActionEvent> getOnAddAction() {
        return onAddActionProperty().get();
    }

    private ObjectProperty<EventHandler<ActionEvent>> onAddAction = new ObjectPropertyBase<EventHandler<ActionEvent>>() {
        @Override
        protected void invalidated() {
            setEventHandler(ActionEvent.ACTION, get());
        }

        @Override
        public Object getBean() {
            return FilterableListView.this;
        }

        @Override
        public String getName() {
            return "onAddAction";
        }
    };

    public final ObjectProperty<EventHandler<ActionEvent>> onRefreshActionProperty() {
        return onRefreshAction;
    }

    public final void setOnRefreshAction(EventHandler<ActionEvent> value) {
        onRefreshActionProperty().set(value);
    }

    public final EventHandler<ActionEvent> getOnRefreshAction() {
        return onRefreshActionProperty().get();
    }

    private ObjectProperty<EventHandler<ActionEvent>> onRefreshAction = new ObjectPropertyBase<EventHandler<ActionEvent>>() {
        @Override
        protected void invalidated() {
            setEventHandler(ActionEvent.ACTION, get());
        }

        @Override
        public Object getBean() {
            return FilterableListView.this;
        }

        @Override
        public String getName() {
            return "onRefreshAction";
        }
    };

    public void setListComparator(Comparator<T> comparator) {
        this.sortedList.setComparator(comparator);
    }

    public void setStringifierFunction(Function<T, String> stringifierFunction) {
        this.stringifierFunction = stringifierFunction;
    }

    private EventHandler<? super KeyEvent> generateListEventHandler() {
        KeyCodeCombination copyCombination = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);

        return keyEvent -> {
            if (keyEvent.getSource() instanceof ListView) {
                if (copyCombination.match(keyEvent)) {
                    String selectedItem = getSelectedItemStringified(getSelectedItem());
                    SystemUtils.copyStringSelectionToClipboard(() -> selectedItem);
                }
            }
        };
    }

    public BooleanProperty refreshButtonDisableProperty(){
        return refreshButton.disableProperty();
    }

    public BooleanProperty addButtonDisableProperty(){
        return refreshButton.disableProperty();
    }

    public BooleanProperty filterTextFieldDisableProperty(){
        return filtertextField.disableProperty();
    }

    private String getSelectedItemStringified(T selectedItem) {
        return stringifierFunction.apply(selectedItem);
    }

    public T getSelectedItem() {
        return listView.getSelectionModel().getSelectedItem();
    }
}
