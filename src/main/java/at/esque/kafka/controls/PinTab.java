package at.esque.kafka.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import org.kordamp.ikonli.javafx.FontIcon;

public class PinTab extends Tab {

    private BooleanProperty pinned = new SimpleBooleanProperty(false);

    public PinTab() {
        super();
        setupGraphic();
        setupContextMenu();
    }

    public PinTab(String s) {
        super(s);
        setupGraphic();
        setupContextMenu();
    }

    public PinTab(String s, Node node) {
        super(s, node);
        setupGraphic();
        setupContextMenu();
    }

    private void setupGraphic(){
        FontIcon pinIcon = new FontIcon();
        pinIcon.setIconLiteral("fa-map-pin");
        pinIcon.setIconSize(10);
        pinIcon.visibleProperty().bind(pinned);
        this.setGraphic(pinIcon);
    }

    private void setupContextMenu(){
        ContextMenu contextMenu = new ContextMenu();
        MenuItem pinMenuItem = new MenuItem("pin");
        MenuItem unpinMenuItem = new MenuItem("unpin");
        pinMenuItem.visibleProperty().bind(pinned.not());
        unpinMenuItem.visibleProperty().bind(pinned);
        pinMenuItem.setOnAction(actionEvent -> pinned.set(true));
        unpinMenuItem.setOnAction(actionEvent -> pinned.set(false));
        contextMenu.getItems().addAll(pinMenuItem, unpinMenuItem);
        this.setContextMenu(contextMenu);
    }

    public boolean isPinned() {
        return pinned.get();
    }

    public BooleanProperty pinnedProperty() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned.set(pinned);
    }
}
