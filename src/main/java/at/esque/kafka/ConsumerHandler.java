package at.esque.kafka;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.cluster.ClusterConfig;
import com.google.inject.Singleton;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
public class ConsumerHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);

    private ConsumerHandler instance;
    private Map<UUID, KafkaConsumer<String, String>> registeredConsumers = new ConcurrentHashMap<>();

    public ConsumerHandler() {
    }

    public ConsumerHandler getInstance() {
        if (instance == null) {
            return instance = new ConsumerHandler();
        }
        return instance;
    }

    public Optional<KafkaConsumer<String, String>> getConsumer(UUID consumerId) {
        return Optional.ofNullable(registeredConsumers.get(consumerId));
    }

    public Map<UUID, KafkaConsumer<String, String>> getRegisteredConsumers() {
        return registeredConsumers;
    }

    public void setRegisteredConsumers(Map<UUID, KafkaConsumer<String, String>> registeredConsumers) {
        this.registeredConsumers = registeredConsumers;
    }

    public UUID registerConsumer(ClusterConfig config) {
        Properties consumerProps = new Properties();
        consumerProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootStrapServers());
        UUID consumerId = UUID.randomUUID();
        consumerProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "kafkaesque-" + consumerId);
        consumerProps.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        LOGGER.info("Creating new Consumer with properties: [{}]", consumerProps);
        registeredConsumers.put(consumerId, new KafkaConsumer<>(consumerProps));
        return consumerId;
    }

    public void deregisterConsumer(UUID consumerId) {
        KafkaConsumer deregisteredConsumer = registeredConsumers.get(consumerId);
        deregisteredConsumer.close();
        registeredConsumers.remove(consumerId);
        LOGGER.info("Deregistered consumer with id [{}]", consumerId);
    }

    public void subscribe(UUID consumerId, String topic) {
        subscribe(registeredConsumers.get(consumerId), topic);
    }

    public void subscribe(KafkaConsumer<String, String> consumer, String topic) {
        consumer.subscribe(Collections.singletonList(topic));
        consumer.poll(0);
    }

    public Map<TopicPartition, Long> getMaxOffsets(UUID consumerId) {
        KafkaConsumer<String, String> currentConsumer = registeredConsumers.get(consumerId);
        return getMaxOffsets(currentConsumer);
    }

    public Map<TopicPartition, Long> getMaxOffsets(KafkaConsumer<String, String> currentConsumer) {
        return currentConsumer.endOffsets(currentConsumer.assignment());
    }

    public Map<TopicPartition, Long> getMinOffsets(UUID consumerId) {
        KafkaConsumer<String, String> currentConsumer = registeredConsumers.get(consumerId);
        return getMinOffsets(currentConsumer);
    }

    public Map<TopicPartition, Long> getMinOffsets(KafkaConsumer<String, String> currentConsumer) {
        return currentConsumer.beginningOffsets(currentConsumer.assignment());
    }

    public void seekToOffset(UUID consumerId, long offset) {
        seekToOffset(registeredConsumers.get(consumerId), offset);
    }

    public void seekToOffset(KafkaConsumer<String, String> consumer, long offset) {
        if (offset == -1) {
            consumer.seekToBeginning(consumer.assignment());
        } else if (offset == -2) {
            consumer.seekToEnd(consumer.assignment());
        } else {
            consumer.assignment().forEach(topicPartition -> consumer.seek(topicPartition, offset));
        }
        try {
            consumer.assignment().forEach(topicPartition -> LOGGER.info("Set position for topicPartition[{}/{}] to [{}]", topicPartition.topic(), topicPartition.partition(), consumer.position(topicPartition, Duration.ofSeconds(10))));
        } catch (Exception e) {
            ErrorAlert.show(e);
        }
    }

    public void seekToTime(UUID consumerId, Long timestamp) {
        seekToTime(registeredConsumers.get(consumerId), timestamp);
    }

    public void seekToTime(KafkaConsumer<String, String> consumer, Long timestamp) {
        Map<TopicPartition, Long> map = consumer.assignment().stream()
                .collect(Collectors.toMap((topicPartition -> topicPartition), topicPartition -> timestamp));
        Map<TopicPartition, OffsetAndTimestamp> offsetAndTimestampMap = consumer.offsetsForTimes(map);
        offsetAndTimestampMap.forEach(((topicPartition, offsetAndTimestamp) -> {
            if (offsetAndTimestamp != null) {
                consumer.seek(topicPartition, offsetAndTimestamp.offset());
            } else {
                consumer.seekToEnd(Collections.singletonList(topicPartition));
            }

        }));
        try {
            consumer.assignment().forEach(topicPartition -> LOGGER.info("Set position for topicPartition[{}/{}] to [{}]", topicPartition.topic(), topicPartition.partition(), consumer.position(topicPartition, Duration.ofSeconds(10))));
        } catch (Exception e) {
            ErrorAlert.show(e);
        }
    }

}
