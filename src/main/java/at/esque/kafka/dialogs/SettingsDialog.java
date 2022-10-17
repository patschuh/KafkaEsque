package at.esque.kafka.dialogs;

import at.esque.kafka.Main;
import at.esque.kafka.MessageType;
import at.esque.kafka.handlers.Settings;
import com.dlsc.formsfx.model.structure.DataField;
import com.dlsc.formsfx.model.structure.Element;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Group;
import com.dlsc.formsfx.model.structure.SingleSelectionField;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.util.StringConverter;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SettingsDialog {

    private SettingsDialog() {
    }

    public static Optional<Map<String, String>> show(Map<String, String> existingConfig) {
        Form form = Form.of(
                Group.of(
                        Field.ofBooleanType(Boolean.parseBoolean(existingConfig.getOrDefault(Settings.USE_SYSTEM_MENU_BAR, Settings.USE_SYSTEM_MENU_BAR_DEFAULT)))
                                .label(Settings.USE_SYSTEM_MENU_BAR)
                                .tooltip(Settings.USE_SYSTEM_MENU_BAR)
                                .placeholder(Settings.USE_SYSTEM_MENU_BAR)
                                .id(Settings.USE_SYSTEM_MENU_BAR)
                ),
                Group.of(
                        Field.ofSingleSelectionType(Arrays.stream(MessageType.values()).map(MessageType::name).collect(Collectors.toList()))
                                .select(Arrays.stream(MessageType.values()).map(MessageType::name).collect(Collectors.toList()).indexOf(existingConfig.getOrDefault(Settings.DEFAULT_KEY_MESSAGE_TYPE, Settings.DEFAULT_KEY_MESSAGE_TYPE_DEFAULT)))
                                .label(Settings.DEFAULT_KEY_MESSAGE_TYPE)
                                .tooltip(Settings.DEFAULT_KEY_MESSAGE_TYPE)
                                .placeholder(Settings.DEFAULT_KEY_MESSAGE_TYPE)
                                .id(Settings.DEFAULT_KEY_MESSAGE_TYPE)
                                .required("This field is required"),
                        Field.ofSingleSelectionType(Arrays.stream(MessageType.values()).map(MessageType::name).collect(Collectors.toList()))
                                .select(Arrays.stream(MessageType.values()).map(MessageType::name).collect(Collectors.toList()).indexOf(existingConfig.getOrDefault(Settings.DEFAULT_VALUE_MESSAGE_TYPE, Settings.DEFAULT_VALUE_MESSAGE_TYPE_DEFAULT)))
                                .label(Settings.DEFAULT_VALUE_MESSAGE_TYPE)
                                .tooltip(Settings.DEFAULT_VALUE_MESSAGE_TYPE)
                                .placeholder(Settings.DEFAULT_VALUE_MESSAGE_TYPE)
                                .id(Settings.DEFAULT_VALUE_MESSAGE_TYPE)
                                .required("This field is required"),
                        Field.ofBooleanType(Boolean.parseBoolean(existingConfig.getOrDefault(Settings.ENABLE_AVRO_LOGICAL_TYPE_CONVERSIONS, Settings.ENABLE_AVRO_LOGICAL_TYPE_CONVERSIONS_DEFAULT)))
                                .label("(experimantal) " + Settings.ENABLE_AVRO_LOGICAL_TYPE_CONVERSIONS)
                                .tooltip("(experimantal) " + Settings.ENABLE_AVRO_LOGICAL_TYPE_CONVERSIONS)
                                .placeholder(Settings.ENABLE_AVRO_LOGICAL_TYPE_CONVERSIONS)
                                .id(Settings.ENABLE_AVRO_LOGICAL_TYPE_CONVERSIONS)
                ),
                Group.of(
                        Field.ofStringType(existingConfig.getOrDefault(Settings.RECENT_TRACE_MAX_ENTRIES, Settings.RECENT_TRACE_MAX_ENTRIES_DEFAULT))
                                .label(Settings.RECENT_TRACE_MAX_ENTRIES)
                                .tooltip(Settings.RECENT_TRACE_MAX_ENTRIES)
                                .placeholder(Settings.RECENT_TRACE_MAX_ENTRIES)
                                .id(Settings.RECENT_TRACE_MAX_ENTRIES)
                                .required("This field is required")
                                .format(new NullFormatStringConverter()),
                        Field.ofBooleanType(Boolean.parseBoolean(existingConfig.getOrDefault(Settings.TRACE_QUICK_SELECT_ENABLED, Settings.TRACE_QUICK_SELECT_ENABLED_DEFAULT)))
                                .label(Settings.TRACE_QUICK_SELECT_ENABLED)
                                .tooltip(Settings.TRACE_QUICK_SELECT_ENABLED)
                                .placeholder(Settings.TRACE_QUICK_SELECT_ENABLED)
                                .id(Settings.TRACE_QUICK_SELECT_ENABLED),
                        Field.ofStringType(existingConfig.getOrDefault(Settings.TRACE_QUICK_SELECT_DURATION_LIST, Settings.TRACE_QUICK_SELECT_DURATION_LIST_DEFAULT))
                                .label(Settings.TRACE_QUICK_SELECT_DURATION_LIST)
                                .tooltip(Settings.TRACE_QUICK_SELECT_DURATION_LIST)
                                .placeholder(Settings.TRACE_QUICK_SELECT_DURATION_LIST)
                                .id(Settings.TRACE_QUICK_SELECT_DURATION_LIST)
                                .required("This field is required")
                                .format(new NullFormatStringConverter())
                ),
                Group.of(
                        Field.ofBooleanType(Boolean.parseBoolean(existingConfig.getOrDefault(Settings.SYNTAX_HIGHLIGHT_THRESHOLD_ENABLED, Settings.SYNTAX_HIGHLIGHT_THRESHOLD_ENABLED_DEFAULT)))
                                .label(Settings.SYNTAX_HIGHLIGHT_THRESHOLD_ENABLED)
                                .tooltip(Settings.SYNTAX_HIGHLIGHT_THRESHOLD_ENABLED)
                                .placeholder(Settings.SYNTAX_HIGHLIGHT_THRESHOLD_ENABLED)
                                .id(Settings.SYNTAX_HIGHLIGHT_THRESHOLD_ENABLED),
                        Field.ofStringType(existingConfig.getOrDefault(Settings.SYNTAX_HIGHLIGHT_THRESHOLD_CHARACTERS, Settings.SYNTAX_HIGHLIGHT_THRESHOLD_CHARACTERS_DEFAULT))
                                .label(Settings.SYNTAX_HIGHLIGHT_THRESHOLD_CHARACTERS)
                                .tooltip(Settings.SYNTAX_HIGHLIGHT_THRESHOLD_CHARACTERS)
                                .placeholder(Settings.SYNTAX_HIGHLIGHT_THRESHOLD_CHARACTERS)
                                .id(Settings.SYNTAX_HIGHLIGHT_THRESHOLD_CHARACTERS)
                                .required("This field is required")
                                .format(new NullFormatStringConverter())
                )
        ).title("Settings");
        Dialog<Map<String, String>> dialog = new Dialog<>();
        Main.applyIcon(dialog);
        dialog.setTitle("Settings");

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);


        Node applyButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        applyButton.getStyleClass().add("primary");

        applyButton.disableProperty().bind(form.validProperty().not());

        FormRenderer formRenderer = new FormRenderer(form);
        formRenderer.setPrefWidth(1000);
        dialog.getDialogPane().setContent(formRenderer);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return form.getFields().stream()
                        .collect(Collectors.toMap(Element::getID, SettingsDialog::extractValue));
            }
            return null;
        });

        return dialog.showAndWait();

    }

    private static String extractValue(Field field) {
        if (field instanceof DataField) {
            return ((DataField) field).getValue().toString();
        } else if (field instanceof SingleSelectionField) {
            return ((SingleSelectionField<?>) field).getSelection().toString();
        } else {
            return "";
        }
    }

    private static class NullFormatStringConverter extends StringConverter<String> {
        public NullFormatStringConverter() {
        }

        @Override
        public String toString(String s) {
            if (s == null || "null".equals(s)) {
                return "";
            }
            return s;
        }

        @Override
        public String fromString(String s) {
            if ("".equals(s) || "null".equals(s)) {
                return null;
            }
            return s;
        }
    }
}
