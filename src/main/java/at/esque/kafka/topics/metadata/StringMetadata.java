package at.esque.kafka.topics.metadata;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableStringValue;

public class StringMetadata implements MessageMetaData<String> {

    private StringProperty name = new SimpleStringProperty();
    private StringProperty value = new SimpleStringProperty();

    public StringMetadata(String name, String value) {
        this.name.set(name);
        this.value.set(value);
    }

    public String getName() {
        return name.get();
    }

    @Override
    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getValue() {
        return value.get();
    }

    @Override
    public Property<String> valueProperty() {
        return value;
    }

    @Override
    public ObservableStringValue valueAsString() {
        return value;
    }

    public void setValue(String value) {
        this.value.set(value);
    }
}
