package at.esque.kafka.topics;

import javafx.beans.property.*;
import javafx.collections.ObservableList;
import org.apache.kafka.common.header.Header;

public class KafkaMessage {

    private LongProperty offset = new SimpleLongProperty();
    private IntegerProperty partition = new SimpleIntegerProperty();
    private StringProperty key = new SimpleStringProperty();
    private StringProperty value = new SimpleStringProperty();
    private StringProperty timestamp = new SimpleStringProperty();
    private ListProperty<Header> headers = new SimpleListProperty<>();

    public long getOffset() {
        return offset.get();
    }

    public LongProperty offsetProperty() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset.set(offset);
    }

    public int getPartition() {
        return partition.get();
    }

    public IntegerProperty partitionProperty() {
        return partition;
    }

    public void setPartition(int partition) {
        this.partition.set(partition);
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

    public String getTimestamp() {
        return timestamp.get();
    }

    public StringProperty timestampProperty() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp.set(timestamp);
    }

    public ObservableList<Header> getHeaders() {
        return headers.get();
    }

    public ListProperty<Header> headersProperty() {
        return headers;
    }

    public void setHeaders(ObservableList<Header> headers) {
        this.headers.set(headers);
    }
}
