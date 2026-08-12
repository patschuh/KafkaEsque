package at.esque.kafka.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class MessagePayloadStoreManager {
    private static final Path TEMP_DIRECTORY = Path.of(System.getProperty("user.home"), ".kafkaesque", "tmp");
    private static final Set<MessagePayloadStore> OPEN_STORES = ConcurrentHashMap.newKeySet();

    private MessagePayloadStoreManager() {
    }

    public static MessagePayloadStore createStore() {
        try {
            Files.createDirectories(TEMP_DIRECTORY);
            MessagePayloadStore store = new MessagePayloadStore(Files.createTempFile(TEMP_DIRECTORY, "messages-", ".bin"));
            OPEN_STORES.add(store);
            return store;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create temporary message store", e);
        }
    }

    static void storeClosed(MessagePayloadStore store) {
        OPEN_STORES.remove(store);
    }

    public static void cleanupStaleStores() {
        if (!Files.isDirectory(TEMP_DIRECTORY)) {
            return;
        }
        try (var paths = Files.walk(TEMP_DIRECTORY)) {
            paths.filter(path -> !path.equals(TEMP_DIRECTORY))
                    .sorted(Comparator.reverseOrder())
                    .forEach(MessagePayloadStoreManager::deleteQuietly);
        } catch (IOException ignored) {
            // A later store creation will report an actionable error if the directory is unusable.
        }
    }

    public static void closeAll() {
        for (MessagePayloadStore store : OPEN_STORES.toArray(MessagePayloadStore[]::new)) {
            store.close();
        }
        cleanupStaleStores();
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
