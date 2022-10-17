package at.esque.kafka.serialization.logicaltypes;

import io.confluent.kafka.serializers.AvroData;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.data.TimeConversions;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

// contains conversions to make non numeric logical types (e.g: uuid) return Strings, this is required to make sure that the toString method of genericRecords produces a valid json string
public class KafkaEsqueConversions {

    public static class UUIDConversion extends Conversion<String> {

        @Override
        public Class<String> getConvertedType() {
            return String.class;
        }

        @Override
        public Schema getRecommendedSchema() {
            return LogicalTypes.uuid().addToSchema(Schema.create(Schema.Type.STRING));
        }

        @Override
        public String getLogicalTypeName() {
            return "uuid";
        }

        @Override
        public String fromCharSequence(CharSequence value, Schema schema, LogicalType type) {
            return value.toString();
        }

        @Override
        public CharSequence toCharSequence(String value, Schema schema, LogicalType type) {
            return value.toString();
        }
    }

    public static class DateConversion extends Conversion<String> {

        @Override
        public Class<String> getConvertedType() {
            return String.class;
        }

        @Override
        public String getLogicalTypeName() {
            return "date";
        }

        @Override
        public String fromInt(Integer daysFromEpoch, Schema schema, LogicalType type) {
            return LocalDate.ofEpochDay(daysFromEpoch).format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        @Override
        public Integer toInt(String date, Schema schema, LogicalType type) {
            long epochDays = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay();

            return (int) epochDays;
        }

        @Override
        public Schema getRecommendedSchema() {
            return LogicalTypes.date().addToSchema(Schema.create(Schema.Type.INT));
        }
    }

    public static class TimeMillisConversion extends Conversion<String> {
        @Override
        public Class<String> getConvertedType() {
            return String.class;
        }

        @Override
        public String getLogicalTypeName() {
            return "time-millis";
        }

        @Override
        public String fromInt(Integer millisFromMidnight, Schema schema, LogicalType type) {
            return LocalTime.ofNanoOfDay(TimeUnit.MILLISECONDS.toNanos(millisFromMidnight)).format(DateTimeFormatter.ISO_LOCAL_TIME);
        }

        @Override
        public Integer toInt(String time, Schema schema, LogicalType type) {
            return (int) TimeUnit.NANOSECONDS.toMillis(LocalTime.parse(time, DateTimeFormatter.ISO_LOCAL_TIME).toNanoOfDay());
        }

        @Override
        public Schema getRecommendedSchema() {
            return LogicalTypes.timeMillis().addToSchema(Schema.create(Schema.Type.INT));
        }
    }

    public static class TimeMicrosConversion extends Conversion<String> {
        @Override
        public Class<String> getConvertedType() {
            return String.class;
        }

        @Override
        public String getLogicalTypeName() {
            return "time-micros";
        }

        @Override
        public String fromLong(Long microsFromMidnight, Schema schema, LogicalType type) {
            return LocalTime.ofNanoOfDay(TimeUnit.MICROSECONDS.toNanos(microsFromMidnight)).format(DateTimeFormatter.ISO_LOCAL_TIME);
        }

        @Override
        public Long toLong(String time, Schema schema, LogicalType type) {
            return TimeUnit.NANOSECONDS.toMicros(LocalTime.parse(time, DateTimeFormatter.ISO_LOCAL_TIME).toNanoOfDay());
        }

        @Override
        public Schema getRecommendedSchema() {
            return LogicalTypes.timeMicros().addToSchema(Schema.create(Schema.Type.LONG));
        }
    }

    public static class TimestampMillisConversion extends Conversion<String> {
        @Override
        public Class<String> getConvertedType() {
            return String.class;
        }

        @Override
        public String getLogicalTypeName() {
            return "timestamp-millis";
        }

        @Override
        public String adjustAndSetValue(String varName, String valParamName) {
            return varName + " = " + valParamName + ".truncatedTo(java.time.temporal.ChronoUnit.MILLIS);";
        }

        @Override
        public String fromLong(Long millisFromEpoch, Schema schema, LogicalType type) {
            return Instant.ofEpochMilli(millisFromEpoch).toString();
        }

        @Override
        public Long toLong(String timestamp, Schema schema, LogicalType type) {
            return Instant.parse(timestamp).toEpochMilli();
        }

        @Override
        public Schema getRecommendedSchema() {
            return LogicalTypes.timestampMillis().addToSchema(Schema.create(Schema.Type.LONG));
        }
    }

    public static class TimestampMicrosConversion extends Conversion<String> {
        @Override
        public Class<String> getConvertedType() {
            return String.class;
        }

        @Override
        public String getLogicalTypeName() {
            return "timestamp-micros";
        }

        @Override
        public String adjustAndSetValue(String varName, String valParamName) {
            return varName + " = " + valParamName + ".truncatedTo(java.time.temporal.ChronoUnit.MICROS);";
        }

        @Override
        public String fromLong(Long microsFromEpoch, Schema schema, LogicalType type) {
            long epochSeconds = microsFromEpoch / (1_000_000L);
            long nanoAdjustment = (microsFromEpoch % (1_000_000L)) * 1_000L;

            return Instant.ofEpochSecond(epochSeconds, nanoAdjustment).toString();
        }

        @Override
        public Long toLong(String string, Schema schema, LogicalType type) {
            Instant instant = Instant.parse(string);
            long seconds = instant.getEpochSecond();
            int nanos = instant.getNano();

            if (seconds < 0 && nanos > 0) {
                long micros = Math.multiplyExact(seconds + 1, 1_000_000L);
                long adjustment = (nanos / 1_000L) - 1_000_000;

                return Math.addExact(micros, adjustment);
            } else {
                long micros = Math.multiplyExact(seconds, 1_000_000L);

                return Math.addExact(micros, nanos / 1_000L);
            }
        }

        @Override
        public Schema getRecommendedSchema() {
            return LogicalTypes.timestampMicros().addToSchema(Schema.create(Schema.Type.LONG));
        }
    }

    public static class LocalTimestampMillisConversion extends Conversion<String> {
        private final TimeConversions.TimestampMillisConversion timestampMillisConversion = new TimeConversions.TimestampMillisConversion();

        @Override
        public Class<String> getConvertedType() {
            return String.class;
        }

        @Override
        public String getLogicalTypeName() {
            return "local-timestamp-millis";
        }

        @Override
        public String fromLong(Long millisFromEpoch, Schema schema, LogicalType type) {
            Instant instant = timestampMillisConversion.fromLong(millisFromEpoch, schema, type);
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        @Override
        public Long toLong(String timestamp, Schema schema, LogicalType type) {
            Instant instant = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC);
            return timestampMillisConversion.toLong(instant, schema, type);
        }

        @Override
        public Schema getRecommendedSchema() {
            return LogicalTypes.localTimestampMillis().addToSchema(Schema.create(Schema.Type.LONG));
        }
    }

    public static class LocalTimestampMicrosConversion extends Conversion<String> {
        private final TimeConversions.TimestampMicrosConversion timestampMicrosConversion = new TimeConversions.TimestampMicrosConversion();

        @Override
        public Class<String> getConvertedType() {
            return String.class;
        }

        @Override
        public String getLogicalTypeName() {
            return "local-timestamp-micros";
        }

        @Override
        public String fromLong(Long microsFromEpoch, Schema schema, LogicalType type) {
            Instant instant = timestampMicrosConversion.fromLong(microsFromEpoch, schema, type);
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        @Override
        public Long toLong(String timestamp, Schema schema, LogicalType type) {
            Instant instant = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC);
            return timestampMicrosConversion.toLong(instant, schema, type);
        }

        @Override
        public Schema getRecommendedSchema() {
            return LogicalTypes.localTimestampMicros().addToSchema(Schema.create(Schema.Type.LONG));
        }
    }

    public static void load(){
        AvroData.getGenericData().addLogicalTypeConversion(new UUIDConversion());
        AvroData.getGenericData().addLogicalTypeConversion(new DateConversion());
        AvroData.getGenericData().addLogicalTypeConversion(new TimeMillisConversion());
        AvroData.getGenericData().addLogicalTypeConversion(new TimeMicrosConversion());
        AvroData.getGenericData().addLogicalTypeConversion(new TimestampMillisConversion());
        AvroData.getGenericData().addLogicalTypeConversion(new TimestampMicrosConversion());
        AvroData.getGenericData().addLogicalTypeConversion(new LocalTimestampMillisConversion());
        AvroData.getGenericData().addLogicalTypeConversion(new LocalTimestampMicrosConversion());
    }
}
