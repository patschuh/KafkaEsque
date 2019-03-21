package at.esque.kafka.cluster;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class CrossClusterOperation {
    private ClusterConfig fromCluster;
    private ClusterConfig toCluster;
    private TopicMessageTypeConfig fromTopic;
    private TopicMessageTypeConfig toTopic;
    private Predicate<ConsumerRecord> filterFunction;
    private AtomicBoolean stop = new AtomicBoolean(false);
    private UUID operationId;

    private StringProperty status = new SimpleStringProperty("Created");
    private LongProperty startTimestampMs = new SimpleLongProperty(-1);
    private LongProperty limit = new SimpleLongProperty(-1);

    public CrossClusterOperation(ClusterConfig fromCluster, ClusterConfig toCluster, TopicMessageTypeConfig fromTopic, TopicMessageTypeConfig toTopic, Predicate<ConsumerRecord> filterFunction) {
        setFromCluster(fromCluster);
        setToCluster(toCluster);
        setFromTopic(fromTopic);
        setToTopic(toTopic);
        this.filterFunction = filterFunction;
    }

    public ClusterConfig getFromCluster() {
        return fromCluster;
    }

    public void setFromCluster(ClusterConfig fromCluster) {
        this.fromCluster = fromCluster;
    }

    public ClusterConfig getToCluster() {
        return toCluster;
    }

    public void setToCluster(ClusterConfig toCluster) {
        this.toCluster = toCluster;
    }

    public TopicMessageTypeConfig getFromTopic() {
        return fromTopic;
    }

    public void setFromTopic(TopicMessageTypeConfig fromTopic) {
        this.fromTopic = fromTopic;
    }

    public TopicMessageTypeConfig getToTopic() {
        return toTopic;
    }

    public void setToTopic(TopicMessageTypeConfig toTopic) {
        this.toTopic = toTopic;
    }

    public Predicate<ConsumerRecord> getFilterFunction() {
        return filterFunction;
    }

    public void setFilterFunction(Predicate<ConsumerRecord> filterFunction) {
        this.filterFunction = filterFunction;
    }

    public UUID getOperationId() {
        return operationId;
    }

    public void setOperationId(UUID operationId) {
        this.operationId = operationId;
    }

    public AtomicBoolean getStop() {
        return stop;
    }

    public void setStop(AtomicBoolean stop) {
        this.stop = stop;
    }

    public String getStatus() {
        return status.get();
    }

    public StringProperty statusProperty() {
        return status;
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    public long getStartTimestampMs() {
        return startTimestampMs.get();
    }

    public LongProperty startTimestampMsProperty() {
        return startTimestampMs;
    }

    public void setStartTimestampMs(long startTimestampMs) {
        this.startTimestampMs.set(startTimestampMs);
    }

    public long getLimit() {
        return limit.get();
    }

    public LongProperty limitProperty() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit.set(limit);
    }
}
