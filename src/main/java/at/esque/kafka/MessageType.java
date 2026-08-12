package at.esque.kafka;

public enum MessageType {
    STRING("STRING (UTF-8)"),
    STRING_ISO_8859_1("STRING (ISO-8859-1, byte safe)"),
    AVRO,
    AVRO_TOPIC_RECORD_NAME_STRATEGY,
    PROTOBUF_SR,
    BASE64,
    UUID,
    SHORT,
    INTEGER,
    LONG,
    FLOAT,
    DOUBLE,
    BYTEARRAY,
    BYTEBUFFER,
    BYTES;

    private final String displayName;

    MessageType() {
        this.displayName = name();
    }

    MessageType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
