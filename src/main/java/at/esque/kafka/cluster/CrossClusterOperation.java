package at.esque.kafka.cluster;

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

    public CrossClusterOperation(ClusterConfig fromCluster, ClusterConfig toCluster, TopicMessageTypeConfig fromTopic, TopicMessageTypeConfig toTopic, Predicate<ConsumerRecord> filterFunction) {
        this.fromCluster = fromCluster;
        this.toCluster = toCluster;
        this.fromTopic = fromTopic;
        this.toTopic = toTopic;
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

    @Override
    public String toString() {
        return fromCluster.getIdentifier() + " / " + fromTopic.getName() + " --> " + toCluster.getIdentifier() + " / " + toTopic.getName();
    }
}
