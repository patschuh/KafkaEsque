package at.esque.kafka.topics.metadata;

import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableStringValue;

public interface MessageMetaData<T> {
    StringProperty nameProperty();
    Property<T> valueProperty();
    ObservableStringValue valueAsString();
}
