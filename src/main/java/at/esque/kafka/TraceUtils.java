package at.esque.kafka;

import at.esque.kafka.topics.model.KafkaHeaderFilterOption;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import javax.validation.constraints.NotNull;
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
            Pattern pattern = Pattern.compile(regex);
            return (cr) -> {
                if (cr.value() == null) {
                    return false;
                }
                Matcher matcher = pattern.matcher(cr.value().toString());
                return matcher.find();
            };

        }
    }

    public static Predicate<ConsumerRecord> keyPredicate(String search, @NotNull String keyMode) {
        if (keyMode.equals("exact match")) {
            return ((cr) -> StringUtils.equals(cr.key().toString(), search));
        } else/*(keyMode.equals("regex (contains)"))*/ {
            Pattern pattern = Pattern.compile(search);
            return (cr) -> {
                if (cr.key() == null) {
                    return false;
                }
                Matcher matcher = pattern.matcher(cr.key().toString());
                return matcher.find();
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
