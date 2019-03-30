package at.esque.kafka.controls;

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collection;

public class FilterableListView extends VBox {
    @FXML
    private TextField filtertextField;
    @FXML
    private ListView<String> listView;
    @FXML
    private HBox toolbarBox;
    @FXML
    private Button addButton;
    @FXML
    private Button refreshButton;

    private ObservableList<String> baseList;
    private FilteredList<String> filteredList;
    private SortedList<String> sortedList;

    public BooleanProperty addButtonVisible = new SimpleBooleanProperty(true);
    public BooleanProperty refreshButtonVisible = new SimpleBooleanProperty(true);

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
            filteredList.setPredicate(item -> StringUtils.isEmpty(newValue)
                    || StringUtils.containsIgnoreCase(item, newValue));
        });
        sortedList.setComparator(String::compareTo);

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

    public ObservableList<String> getBaseList() {
        return baseList;
    }

    public void setItems(Collection<String> items) {
        baseList.clear();
        baseList.addAll(items);
    }

    public void addItems(Collection<String> items) {
        baseList.addAll(items);
    }

    public ListView<String> getListView() {
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

    public final ObjectProperty<EventHandler<ActionEvent>> onAddActionProperty() { return onAddAction; }
    public final void setOnAddAction(EventHandler<ActionEvent> value) { onAddActionProperty().set(value); }
    public final EventHandler<ActionEvent> getOnAddAction() { return onAddActionProperty().get(); }
    private ObjectProperty<EventHandler<ActionEvent>> onAddAction = new ObjectPropertyBase<EventHandler<ActionEvent>>() {
        @Override protected void invalidated() {
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

    public final ObjectProperty<EventHandler<ActionEvent>> onRefreshActionProperty() { return onRefreshAction; }
    public final void setOnRefreshAction(EventHandler<ActionEvent> value) { onRefreshActionProperty().set(value); }
    public final EventHandler<ActionEvent> getOnRefreshAction() { return onRefreshActionProperty().get(); }
    private ObjectProperty<EventHandler<ActionEvent>> onRefreshAction = new ObjectPropertyBase<EventHandler<ActionEvent>>() {
        @Override protected void invalidated() {
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
}
