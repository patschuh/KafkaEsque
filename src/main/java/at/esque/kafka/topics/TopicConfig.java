package at.esque.kafka.topics;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TopicConfig {
    private StringProperty key = new SimpleStringProperty();
    private StringProperty value = new SimpleStringProperty();

    TopicConfig(String key, String value) {
        this.key.setValue(key);
        this.value.setValue(value);
    }

    public String getKey() {
        return key.get();
    }

    public StringProperty keyProperty() {
        return key;
    }

    public void setKey(String key) {
        this.key.set(key);
    }

    public String getValue() {
        return value.get();
    }

    public StringProperty valueProperty() {
        return value;
    }

    public void setValue(String value) {
        this.value.set(value);
    }

    @Override
    public String toString() {
        return key.get() + " -> " + value.get();
    }
}
