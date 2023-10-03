package at.esque.kafka;

import at.esque.kafka.topics.model.KafkaHeaderFilterOption;
import com.google.protobuf.Message;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TraceUtils {

    private TraceUtils() {
    }


    public static Predicate<ConsumerRecord> valuePredicate(String regex, boolean searchNull) {

        if (searchNull) {
            return ((cr) -> cr.value() == null);
        } else {
            Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
            return (cr) -> {
                if (cr.value() == null) {
                    return false;
                }
                if (cr.value() instanceof Message messageValue) {
                    Matcher matcher = null;
                    try {
                        matcher = pattern.matcher(JsonUtils.toJson(messageValue));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return matcher.find();
                } else {
                    Matcher matcher = pattern.matcher(cr.value().toString());
                    return matcher.find();
                }
            };

        }
    }

    public static Predicate<ConsumerRecord> keyPredicate(String search, @NotNull String keyMode) {
        if (keyMode.equals("exact match")) {
            return ((cr) -> {
                if (cr.key() instanceof Message messageKey) {
                    try {
                        return StringUtils.equals(JsonUtils.toJson(messageKey), search);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return StringUtils.equals(cr.key().toString(), search);
                }
            });
        } else/*(keyMode.equals("regex (contains)"))*/ {
            Pattern pattern = Pattern.compile(search, Pattern.DOTALL);
            return (cr) -> {
                if (cr.key() == null) {
                    return false;
                }
                if (cr.key() instanceof Message messageKey) {
                    Matcher matcher = null;
                    try {
                        matcher = pattern.matcher(JsonUtils.toJson(messageKey));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return matcher.find();
                } else {
                    Matcher matcher = pattern.matcher(cr.key().toString());
                    return matcher.find();
                }
            };
        }
    }

    public static Predicate<ConsumerRecord> consumerRecordHeaderPredicate(KafkaHeaderFilterOption kafkaHeaderFilterOption) {
        return (consumerRecord) -> {
            Headers headers = consumerRecord.headers();
            Stream<Header> stream = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(headers.headers(kafkaHeaderFilterOption.getHeader()).iterator(), Spliterator.ORDERED),
                    false);
            return stream.anyMatch(header -> kafkaHeaderFilterOption.isExactMatch() ? headerValueExactMatch(kafkaHeaderFilterOption, header) : headerValueRegexMatch(kafkaHeaderFilterOption, header));
        };
    }

    private static boolean headerValueExactMatch(KafkaHeaderFilterOption kafkaHeaderFilterOption, Header header) {
        return new String(header.value(), StandardCharsets.UTF_8).equals(kafkaHeaderFilterOption.getFilterString());
    }

    private static boolean headerValueRegexMatch(KafkaHeaderFilterOption kafkaHeaderFilterOption, Header header) {
        Pattern pattern = Pattern.compile(kafkaHeaderFilterOption.getFilterString());
        if (header.value() == null) {
            return false;
        }

        Matcher matcher = pattern.matcher(new String(header.value(), StandardCharsets.UTF_8));
        return matcher.find();
    }
}
