package kafka;

import kafka.server.KafkaServer;
import kafka.server.NotRunning;
import org.apache.kafka.common.utils.Utils;
import org.apache.kafka.test.TestUtils;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;

import java.io.File;
import java.io.IOException;

/**
 * Fixes delete failure bug on Windows.
 * <p>
 * Overriding {@link EmbeddedKafkaRule#after()}, so that all temporary folders are deleted also on
 * Windows.
 * <p>
 * This is achieved by calling getZookeeper().zookeeper().getZKDatabase().close().
 * <p>
 * In addition, the number of try-catch blocks in the original implementation have been refactored to
 * the {@link #swallow(SimpleFunction)} method.
 * <p>
 * The solution is based on following Gist:
 * https://gist.github.com/grofoli/cffa0d06840cff34117d244f2bd7f628
 */
public class WindowsEmbeddedKafkaRule extends EmbeddedKafkaRule {

    public WindowsEmbeddedKafkaRule(int count) {
        super(count);
    }

    public WindowsEmbeddedKafkaRule(int count, boolean controlledShutdown, String... topics) {
        super(count, controlledShutdown, topics);
    }

    public WindowsEmbeddedKafkaRule(int count, boolean controlledShutdown, int partitions, String... topics) {
        super(count, controlledShutdown, partitions, topics);
    }

    @Override
    public void before() {
        /* really brutal hack for logs of topic partitions that have received messages */
        for (File file : new File(TestUtils.tempDirectory().getParent()).listFiles()) {
            if (file.getName().startsWith("kafka-")) {
                swallow(() -> Utils.delete(file));
            }
        }
        super.before();
    }

    @Override
    public void after() {
        EmbeddedKafkaBroker embeddedKafka = getEmbeddedKafka();
        System.getProperties().remove(EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS);
        System.getProperties().remove(EmbeddedKafkaBroker.SPRING_EMBEDDED_ZOOKEEPER_CONNECT);
        for (KafkaServer kafkaServer : embeddedKafka.getKafkaServers()) {
            swallow(() -> shutdown(kafkaServer));
            swallow(() -> deleteLogDir(kafkaServer));
        }
        swallow(this::closeZkClient);
        swallow(this::shutdownZookeeper);
    }

    private void shutdown(KafkaServer kafkaServer) {
        if (kafkaServer.brokerState().get().value() != NotRunning.state()) {
            kafkaServer.shutdown();
            kafkaServer.awaitShutdown();
        }
    }

    private void deleteLogDir(KafkaServer kafkaServer) {
        // no need to make this delete here because of the shutdown hook in Testutils
        // CoreUtils.delete(kafkaServer.config().logDirs());
    }

    private void closeZkClient() {
        getEmbeddedKafka().getZooKeeperClient().close();
    }

    private void shutdownZookeeper() throws IOException {
        getEmbeddedKafka().getZookeeper().shutdown();
    }

    private void swallow(SimpleFunction function) {
        try {
            function.execute();
        } catch (Exception e) {
            // do nothing
        }
    }

    @FunctionalInterface
    interface SimpleFunction {
        void execute() throws Exception;
    }
}
