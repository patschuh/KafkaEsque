package at.esque.kafka.handlers;

import at.esque.kafka.cluster.ClusterConfig;
import at.esque.kafka.cluster.CrossClusterOperation;
import at.esque.kafka.cluster.TopicMessageTypeConfig;
import com.google.inject.Singleton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Singleton
public class CrossClusterOperationHandler {
    private Map<UUID, CrossClusterOperation> registerdOperations = new ConcurrentHashMap<>();

    public UUID registerOperation(CrossClusterOperation crossClusterOperation) {
        UUID operationId = UUID.randomUUID();
        crossClusterOperation.setOperationId(operationId);
        registerdOperations.put(operationId, crossClusterOperation);
        return operationId;
    }

    public UUID registerOperation(ClusterConfig fromCluster, ClusterConfig toCluster, TopicMessageTypeConfig fromTopic, TopicMessageTypeConfig toTopic, Predicate<ConsumerRecord> filterFunction) {
        return registerOperation(new CrossClusterOperation(fromCluster, toCluster, fromTopic, toTopic, filterFunction));
    }

    public CrossClusterOperation getOperation(UUID operationId) {
        return registerdOperations.get(operationId);
    }

    public ObservableList<CrossClusterOperation> getOperations() {
        return FXCollections.observableArrayList(registerdOperations.values());
    }

    /**
     * Sets the stop flag on the operation signaling the deamon thread to stop processing, will also deregister the operation
     *
     * @param operationId
     */
    public void markOperationForStop(UUID operationId) {
        CrossClusterOperation crossClusterOperation = registerdOperations.get(operationId);
        if (crossClusterOperation != null) {
            crossClusterOperation.getStop().set(true);
            registerdOperations.remove(operationId);
        }
    }
}
