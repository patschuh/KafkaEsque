package at.esque.kafka.lag.viewer;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Lag {
    private StringProperty title = new SimpleStringProperty();
    private LongProperty currentOffset = new SimpleLongProperty(0);
    private LongProperty endOffset = new SimpleLongProperty(0);
    private ObservableList<Lag> subEntities= FXCollections.observableArrayList();

    public Lag(){}

    public Lag(String title, long currentOffset, long endOffset) {
        this.title.set(title);
        this.currentOffset.setValue(currentOffset);
        this.endOffset.setValue(endOffset);
    }

    public String getTitle() {
        return title.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public long getCurrentOffset() {
        return currentOffset.get();
    }

    public LongProperty currentOffsetProperty() {
        return currentOffset;
    }

    public void setCurrentOffset(long currentOffset) {
        this.currentOffset.set(currentOffset);
    }

    public long getEndOffset() {
        return endOffset.get();
    }

    public LongProperty endOffsetProperty() {
        return endOffset;
    }

    public void setEndOffset(long endOffset) {
        this.endOffset.set(endOffset);
    }

    public ObservableList<Lag> getSubEntities() {
        return subEntities;
    }

    public void setSubEntities(ObservableList<Lag> subEntities) {
        this.subEntities = subEntities;
    }
}
