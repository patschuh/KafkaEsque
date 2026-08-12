package at.esque.kafka.storage;

import at.esque.kafka.JsonUtils;
import at.esque.kafka.handlers.Settings;
import at.esque.kafka.topics.KafkaMessage;
import javafx.collections.FXCollections;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.Test;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MessagePayloadStoreTest {

    @Test
    public void offloadsFullPayloadAndKeepsBoundedPreview() throws Exception {
        Path path = Files.createTempFile("kafkaesque-message-store-test", ".bin");
        MessagePayloadStore store = new MessagePayloadStore(path);
        try {
            KafkaMessage message = message("key-" + "x".repeat(2048), "😀".repeat(1024));
            byte[] headerValue = new byte[]{0, 1, -1, 42};
            message.setHeaders(FXCollections.observableArrayList(new RecordHeader("binary", headerValue)));

            KafkaMessage preview = store.offload(message, 1024);

            assertTrue(preview.isOffloaded());
            assertTrue(preview.getKey().getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 1024);
            assertTrue(preview.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 1024);
            assertTrue(preview.getHeaders().isEmpty());

            KafkaMessage full = store.materialize(preview);
            assertEquals("key-" + "x".repeat(2048), full.getKey());
            assertEquals("😀".repeat(1024), full.getValue());
            assertEquals("binary", full.getHeaders().get(0).key());
            assertArrayEquals(headerValue, full.getHeaders().get(0).value());
            assertFalse(full.isOffloaded());
        } finally {
            store.close();
        }
        assertFalse(Files.exists(path));
    }

    @Test
    public void preservesNullPayloadsAndStreamsFullJsonExport() throws Exception {
        Path path = Files.createTempFile("kafkaesque-message-store-test", ".bin");
        MessagePayloadStore store = new MessagePayloadStore(path);
        try {
            KafkaMessage preview = store.offload(message(null, "{\"large\":\"" + "v".repeat(2048) + "\"}"), 16);
            StringWriter writer = new StringWriter();

            JsonUtils.writeMessageToJsonFile(java.util.List.of(preview), store::materialize, writer);

            assertNull(store.materialize(preview).getKey());
            assertTrue(writer.toString().contains("v".repeat(2048)));
            assertFalse(writer.toString().contains("payloadOffset"));
            assertFalse(writer.toString().contains("offloaded"));
        } finally {
            store.close();
        }
    }

    @Test
    public void readsPreviewLimitAsBytes() {
        assertEquals(1536, Settings.readMessagePreviewSizeBytes(
                Map.of(Settings.MESSAGE_PREVIEW_SIZE_BYTES, "1536")));
        int defaultSizeBytes = Integer.parseInt(Settings.MESSAGE_PREVIEW_SIZE_BYTES_DEFAULT);
        assertEquals(defaultSizeBytes, Settings.readMessagePreviewSizeBytes(Map.of()));
        assertEquals(defaultSizeBytes, Settings.readMessagePreviewSizeBytes(
                Map.of(Settings.MESSAGE_PREVIEW_SIZE_BYTES, "invalid")));
    }

    private static KafkaMessage message(String key, String value) {
        KafkaMessage message = new KafkaMessage();
        message.setOffset(12);
        message.setPartition(3);
        message.setTimestamp("2026-08-12T00:00:00Z");
        message.setKey(key);
        message.setValue(value);
        return message;
    }
}
