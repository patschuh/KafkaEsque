package at.esque.kafka.storage;

import at.esque.kafka.topics.KafkaMessage;
import javafx.collections.FXCollections;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MessagePayloadStore implements AutoCloseable {
    private final Path path;
    private final RandomAccessFile file;
    private long writePosition;
    private boolean closed;

    MessagePayloadStore(Path path) throws IOException {
        this.path = path;
        this.file = new RandomAccessFile(path.toFile(), "rw");
    }

    public synchronized KafkaMessage offload(KafkaMessage message, int previewSizeBytes) {
        ensureOpen();
        try {
            long offset = writePosition;
            file.seek(offset);
            writeString(message.getKey());
            writeString(message.getValue());
            Header[] headers = message.getHeaders() == null ? new Header[0] : message.getHeaders().toArray(Header[]::new);
            file.writeInt(headers.length);
            for (Header header : headers) {
                writeString(header.key());
                writeBytes(header.value());
            }
            writePosition = file.getFilePointer();
            message.setPayloadReference(this, offset);
            message.setKey(preview(message.getKey(), previewSizeBytes));
            message.setValue(preview(message.getValue(), previewSizeBytes));
            message.setHeaders(FXCollections.observableArrayList());
            return message;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to offload Kafka message", e);
        }
    }

    public synchronized KafkaMessage materialize(KafkaMessage preview) {
        if (preview == null || !preview.isOffloaded()) {
            return preview;
        }
        ensureOpen();
        try {
            file.seek(preview.getPayloadOffset());
            KafkaMessage full = preview.copyMetadata();
            full.setKey(readString());
            full.setValue(readString());
            int headerCount = file.readInt();
            var headers = FXCollections.<Header>observableArrayList();
            for (int i = 0; i < headerCount; i++) {
                headers.add(new RecordHeader(readString(), readBytes()));
            }
            full.setHeaders(headers);
            return full;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load Kafka message payload", e);
        }
    }

    public synchronized void reset() {
        ensureOpen();
        try {
            file.setLength(0);
            file.seek(0);
            writePosition = 0;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to reset temporary message store", e);
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            file.close();
        } catch (IOException ignored) {
        } finally {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
            MessagePayloadStoreManager.storeClosed(this);
        }
    }

    public static String preview(String value, int maxBytes) {
        if (value == null) {
            return value;
        }
        int end = 0;
        int bytes = 0;
        while (end < value.length()) {
            int codePoint = value.codePointAt(end);
            int codePointBytes = utf8Length(codePoint);
            if (bytes + codePointBytes > maxBytes) {
                break;
            }
            bytes += codePointBytes;
            end += Character.charCount(codePoint);
        }
        return end == value.length() ? value : value.substring(0, end);
    }

    private static int utf8Length(int codePoint) {
        if (codePoint <= 0x7f) {
            return 1;
        }
        if (codePoint <= 0x7ff) {
            return 2;
        }
        return codePoint <= 0xffff ? 3 : 4;
    }

    private void writeString(String value) throws IOException {
        writeBytes(value == null ? null : value.getBytes(StandardCharsets.UTF_8));
    }

    private String readString() throws IOException {
        byte[] bytes = readBytes();
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    private void writeBytes(byte[] bytes) throws IOException {
        if (bytes == null) {
            file.writeInt(-1);
        } else {
            file.writeInt(bytes.length);
            file.write(bytes);
        }
    }

    private byte[] readBytes() throws IOException {
        int length = file.readInt();
        if (length == -1) {
            return null;
        }
        if (length < 0 || length > file.length() - file.getFilePointer()) {
            throw new EOFException("Invalid payload length " + length);
        }
        byte[] bytes = new byte[length];
        file.readFully(bytes);
        return bytes;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Temporary message store is closed");
        }
    }
}
