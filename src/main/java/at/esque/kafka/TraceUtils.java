package at.esque.kafka;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import javax.validation.constraints.NotNull;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TraceUtils {


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
        if(keyMode.equals("exact match")) {
            return ((cr) -> StringUtils.equals(cr.key().toString(), search));
        }else/*(keyMode.equals("regex (contains)"))*/{
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

}
