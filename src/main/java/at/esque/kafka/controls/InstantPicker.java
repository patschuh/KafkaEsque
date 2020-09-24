package at.esque.kafka.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Based on tornadofx-controls DateTimePicker: https://github.com/edvin/tornadofx-controls/blob/master/src/main/java/tornadofx/control/DateTimePicker.java
 */
@SuppressWarnings("unused")
public class InstantPicker extends DatePicker {
    public static final String DefaultFormat = "yyyy-MM-dd HH:mm:ss.SSS z";

    private DateTimeFormatter formatter;
    private ObjectProperty<Instant> instantValue = new SimpleObjectProperty<>(Instant.now());
    private ObjectProperty<String> format = new SimpleObjectProperty<String>() {
        public void set(String newValue) {
            super.set(newValue);
            formatter = DateTimeFormatter.ofPattern(newValue);
        }
    };
    private BooleanProperty displayAsEpoch = new SimpleBooleanProperty(false);

    public void alignColumnCountWithFormat() {
        getEditor().setPrefColumnCount(getFormat().length());
    }

    public InstantPicker() {
        setFormat(DefaultFormat);
        setConverter(new InternalConverter());
        alignColumnCountWithFormat();

        valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                instantValue.set(null);
            } else {
                if (instantValue.get() == null) {
                    instantValue.set(LocalDateTime.of(newValue, LocalTime.now()).toInstant(ZoneOffset.UTC));
                } else {
                    LocalTime time = instantValue.get().atOffset(ZoneOffset.UTC).toLocalTime();
                    instantValue.set(LocalDateTime.of(newValue, time).toInstant(ZoneOffset.UTC));
                }
            }
        });

        instantValue.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                OffsetDateTime offsetDateTime = newValue.atOffset(ZoneOffset.UTC);
                LocalDate dateValue = LocalDate.of(offsetDateTime.getYear(), offsetDateTime.getMonth(), offsetDateTime.getDayOfMonth());
                boolean forceUpdate = dateValue.equals(valueProperty().get());
                setValue(dateValue);
                if (forceUpdate) {
                    setConverter(new InternalConverter());
                }
            } else {
                setValue(null);
            }

        });

        displayAsEpoch.addListener((observable, oldValue, newValue) -> {
            setConverter(new InternalConverter());
        });

        getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue)
                simulateEnterPressed();
        });

    }

    private void simulateEnterPressed() {
        getEditor().commitValue();
    }

    public Instant getInstantValue() {
        return instantValue.get();
    }

    public void setInstantValue(Instant instantValue) {
        this.instantValue.set(instantValue);
    }

    public ObjectProperty<Instant> instantValueProperty() {
        return instantValue;
    }

    public boolean isDisplayAsEpoch() {
        return displayAsEpoch.get();
    }

    public BooleanProperty displayAsEpochProperty() {
        return displayAsEpoch;
    }

    public void setDisplayAsEpoch(boolean displayAsEpoch) {
        this.displayAsEpoch.set(displayAsEpoch);
    }

    public String getFormat() {
        return format.get();
    }

    public ObjectProperty<String> formatProperty() {
        return format;
    }

    public void setFormat(String format) {
        this.format.set(format);
        alignColumnCountWithFormat();
    }

    class InternalConverter extends StringConverter<LocalDate> {
        public String toString(LocalDate object) {
            Instant value = getInstantValue();
            return (value != null) ? convertToString(value) : "";
        }

        public LocalDate fromString(String value) {
            if (value == null || value.isEmpty()) {
                instantValue.set(null);
                return null;
            }
            Instant instant = convertFromString(value);
            instantValue.set(instant);
            return instant.atOffset(ZoneOffset.UTC).toLocalDate();
        }

        private Instant convertFromString(String value) {
            if (isDisplayAsEpoch()) {
                return Instant.ofEpochMilli(Long.parseLong(value));
            } else {
                return LocalDateTime.parse(value, formatter).toInstant(ZoneOffset.UTC);
            }
        }

        private String convertToString(Instant value) {
            if (isDisplayAsEpoch()) {
                return value.toEpochMilli() + "";
            } else {
                return value.atZone(ZoneId.of("UTC")).format(formatter);
            }
        }
    }
}
