package at.esque.kafka.dialogs;

import at.esque.kafka.Main;
import at.esque.kafka.alerts.ErrorAlert;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Group;
import com.dlsc.formsfx.model.util.BindingMode;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.client.rest.entities.Config;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import java.util.Optional;

public class SubjectConfigDialog {
    private SubjectConfigDialog() {
    }

    private enum SchemaCompatibilityLevel {
        BACKWARD, BACKWARD_TRANSITIVE, FORWARD, FORWARD_TRANSITIVE, FULL, FULL_TRANSITIVE, NONE, UNDEFINED;
    }

    public static void show(RestService schemaRegistryRestService, String selectedSubject) {

        try {
            //get configured global compatibility level
            StringProperty globalCompatibilityLevel = new SimpleStringProperty();

            Config globalConfig = schemaRegistryRestService.getConfig(null);
            globalCompatibilityLevel.set(globalConfig.getCompatibilityLevel());

            // if configured also get the configured level on subject
            SimpleObjectProperty<SchemaCompatibilityLevel> subjectCompatibilityLevel = new SimpleObjectProperty<>();
            try {
                Config subjectConfig = schemaRegistryRestService.getConfig(selectedSubject);

                SchemaCompatibilityLevel configuredLevel = SchemaCompatibilityLevel.valueOf(subjectConfig.getCompatibilityLevel());

                if (configuredLevel == null) {
                    throw new IllegalArgumentException(String.format("Schema Registry returned an unknown compatibility level (%s)", subjectConfig.getCompatibilityLevel()));
                }

                subjectCompatibilityLevel.set(configuredLevel);
            } catch (RestClientException e) {
                //Nothing configured on subject level
                // 40401 for compatibility with older versions
                if (e.getErrorCode() == 40401 || e.getErrorCode() == 40408) {
                    subjectCompatibilityLevel.set(SchemaCompatibilityLevel.UNDEFINED);
                } else {
                    throw e;
                }
            }

            ListProperty<SchemaCompatibilityLevel> schemaCompatibilityLevels = new SimpleListProperty<>(FXCollections.observableArrayList(SchemaCompatibilityLevel.values()));

            SimpleObjectProperty<SchemaCompatibilityLevel> existingSubjectCompatibilityLevel = new SimpleObjectProperty(subjectCompatibilityLevel.get());

            // Show dialog
            Form form = Form.of(
                            Group.of(
                                    Field.ofStringType("Configured schema compatibility level:")
                                            .editable(false),
                                    Field.ofStringType(globalCompatibilityLevel.getValueSafe())
                                            .label("Global:")
                                            .tooltip("Global Schema Compatibility Level")
                                            .editable(false),
                                    Field.ofSingleSelectionType(schemaCompatibilityLevels)
                                            .label("Subject:")
                                            .tooltip("Subject Schema Compatibility Leve")
                                            .bind(schemaCompatibilityLevels, subjectCompatibilityLevel)
                            )
                    ).title("Schema Compatibility")
                    .binding(BindingMode.CONTINUOUS);

            Dialog<SimpleObjectProperty> dialog = new Dialog<>();
            Main.applyIcon(dialog);
            dialog.setTitle("Subject configuration");

            ButtonType saveConfigButtonType = new ButtonType("Change", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveConfigButtonType, ButtonType.CANCEL);

            Node addClusterButton = dialog.getDialogPane().lookupButton(saveConfigButtonType);
            addClusterButton.getStyleClass().add("primary");

            FormRenderer formRenderer = new FormRenderer(form);
            formRenderer.setPrefWidth(500);
            dialog.getDialogPane().setContent(formRenderer);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveConfigButtonType) {
                    return subjectCompatibilityLevel;
                }
                return null;
            });

            Optional<SimpleObjectProperty> newSubjectLevel = dialog.showAndWait();

            if (newSubjectLevel.isPresent() && !newSubjectLevel.get().get().equals(existingSubjectCompatibilityLevel.getValue())) {
                SchemaCompatibilityLevel newLevel = (SchemaCompatibilityLevel) newSubjectLevel.get().get();
                if (newLevel.equals(SchemaCompatibilityLevel.UNDEFINED)) {
                    schemaRegistryRestService.deleteConfig(selectedSubject);
                } else {
                    schemaRegistryRestService.updateCompatibility(newLevel.name(), selectedSubject);
                }
            }

        } catch (Exception e) {
            ErrorAlert.show(e);
        }
    }
}
