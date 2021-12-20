package at.esque.kafka.cluster;

import at.esque.kafka.MessageType;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TopicMessageTypeConfig {
    private StringProperty name = new SimpleStringProperty();
    private StringProperty keyType = new SimpleStringProperty(MessageType.STRING.name());
    private StringProperty valueType = new SimpleStringProperty(MessageType.STRING.name());

    public TopicMessageTypeConfig() {
    }

    public TopicMessageTypeConfig(String name) {
        this.name.set(name);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public MessageType getKeyType() {
        if (keyType.isEmpty().get()) {
            return null;
        }
        return MessageType.valueOf(keyType.get());
    }

    public StringProperty keyTypeProperty() {
        return keyType;
    }

    public void setKeyType(MessageType keyType) {
        this.keyType.set(keyType.name());
    }

    public MessageType getValueType() {
        if (valueType.isEmpty().get()) {
            return null;
        }
        return MessageType.valueOf(valueType.get());
    }

    public StringProperty valueTypeProperty() {
        return valueType;
    }

    public void setValueType(MessageType valueType) {
        this.valueType.set(valueType.name());
    }

    public boolean containsAvro() {
        return getValueType() == MessageType.AVRO || getValueType() == MessageType.AVRO_TOPIC_RECORD_NAME_STRATEGY || getKeyType() == MessageType.AVRO || getKeyType() == MessageType.AVRO_TOPIC_RECORD_NAME_STRATEGY;
    }
}
