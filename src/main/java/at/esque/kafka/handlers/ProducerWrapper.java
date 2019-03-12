package at.esque.kafka.handlers;

import io.confluent.kafka.schemaregistry.client.rest.RestService;
import org.apache.kafka.clients.producer.KafkaProducer;

public class ProducerWrapper {
    private String clusterId;
    private KafkaProducer producer;
    private RestService schemaRegistryRestService;

    public ProducerWrapper(String clusterId, KafkaProducer producer, RestService schemaRegistryRestService) {
        this.producer = producer;
        this.schemaRegistryRestService = schemaRegistryRestService;
        this.clusterId = clusterId;
    }

    public KafkaProducer getProducer() {
        return producer;
    }

    public void setProducer(KafkaProducer producer) {
        this.producer = producer;
    }

    public RestService getSchemaRegistryRestService() {
        return schemaRegistryRestService;
    }

    public void setSchemaRegistryRestService(RestService schemaRegistryRestService) {
        this.schemaRegistryRestService = schemaRegistryRestService;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }
}
