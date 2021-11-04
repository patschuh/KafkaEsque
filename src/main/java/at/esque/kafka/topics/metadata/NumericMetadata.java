package at.esque.kafka.topics.metadata;

import javafx.beans.binding.StringBinding;
import javafx.beans.property.LongProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableStringValue;

public class NumericMetadata implements MessageMetaData<Number> {

    private StringProperty name = new SimpleStringProperty();
    private LongProperty value = new SimpleLongProperty();

    public NumericMetadata(String name, Long value) {
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

    public long getValue() {
        return value.get();
    }

    @Override
    public Property<Number> valueProperty() {
        return value;
    }

    @Override
    public ObservableStringValue valueAsString() {
        return value.asString();
    }

    public void setValue(long value) {
        this.value.set(value);
    }
}
