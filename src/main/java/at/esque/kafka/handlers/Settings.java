package at.esque.kafka.handlers;

import at.esque.kafka.dialogs.TraceInputDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Settings {
    private static final Logger logger = LoggerFactory.getLogger(TraceInputDialog.class);

    private Settings() {
    }

    public static final String USE_SYSTEM_MENU_BAR = "use.system.menubar";
    public static final String USE_SYSTEM_MENU_BAR_DEFAULT = "true";
    public static final String TRACE_QUICK_SELECT_DURATION_LIST = "trace.quick.select.duration.list";
    public static final String TRACE_QUICK_SELECT_DURATION_LIST_DEFAULT = "PT2H, P1D, P7D";
    public static final String TRACE_QUICK_SELECT_ENABLED = "trace.quick.select.enabled";
    public static final String TRACE_QUICK_SELECT_ENABLED_DEFAULT = "true";
    public static final String SYNTAX_HIGHLIGHT_THRESHOLD_ENABLED = "syntax.highlight.threshold.enabled";
    public static final String SYNTAX_HIGHLIGHT_THRESHOLD_ENABLED_DEFAULT = "true";
    public static final String SYNTAX_HIGHLIGHT_THRESHOLD_CHARACTERS = "syntax.highlight.threshold.characters";
    public static final String SYNTAX_HIGHLIGHT_THRESHOLD_CHARACTERS_DEFAULT = "50000";
    public static final String RECENT_TRACE_MAX_ENTRIES = "recent.trace.max.entries";
    public static final String RECENT_TRACE_MAX_ENTRIES_DEFAULT = "10";

    public static List<Duration> readDurationSetting(Map<String, String> settings) {
        return readDurationSetting(settings.get(TRACE_QUICK_SELECT_DURATION_LIST));
    }

    public static boolean isTraceQuickSelectEnabled(Map<String, String> settings){
        if(settings == null){
            return false;
        }
        return Boolean.parseBoolean(settings.get(TRACE_QUICK_SELECT_ENABLED));
    }

    public static List<Duration> readDurationSetting(String setting) {
        if (setting == null) {
            return new ArrayList<>();
        }
        String[] split = setting.split(",");
        return Arrays.stream(split)
                .map(s -> {
                    try {
                        return Duration.parse(s.trim());
                    } catch (Exception e) {
                        logger.error("Failed to parse Duration", e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(duration -> !duration.isNegative())
                .collect(Collectors.toList());
    }

}
