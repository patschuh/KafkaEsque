package at.esque.kafka.dialogs;

import at.esque.kafka.Main;
import at.esque.kafka.acl.viewer.Acl;
import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.cluster.KafkaesqueAdminClient;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Group;
import com.dlsc.formsfx.model.util.BindingMode;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;

import java.util.Arrays;
import java.util.Optional;

public class CreateACLDialog {

    private CreateACLDialog() {
    }

    public static void show(KafkaesqueAdminClient kafkaesqueAdminClient) {

        try {

            ListProperty<ResourceType> resourceTypes = new SimpleListProperty<>(FXCollections.observableArrayList(ResourceType.values()));
            ListProperty<PatternType> patternTypes = new SimpleListProperty<>(FXCollections.observableArrayList(PatternType.values()));
            ListProperty<AclOperation> aclOperations = new SimpleListProperty<>(FXCollections.observableArrayList(AclOperation.values()));
            ListProperty<AclPermissionType> permissionTypes = new SimpleListProperty<>(FXCollections.observableArrayList(AclPermissionType.values()));

            SimpleObjectProperty<ResourceType> selectedResourceType = new SimpleObjectProperty<>(ResourceType.TOPIC);
            SimpleStringProperty selectedResourceName = new SimpleStringProperty("");
            SimpleObjectProperty<PatternType> selectedPatternType = new SimpleObjectProperty<>(PatternType.LITERAL);
            SimpleStringProperty selectedPrincipal = new SimpleStringProperty("");
            SimpleStringProperty selectedHost = new SimpleStringProperty("");
            SimpleObjectProperty<AclOperation> seletedAclOperation = new SimpleObjectProperty<>(AclOperation.ALL);
            SimpleObjectProperty<AclPermissionType> selectedPermissionType = new SimpleObjectProperty<>(AclPermissionType.ALLOW);

            // Show dialog
            Form form = Form.of(
                    Group.of(
                            Field.ofSingleSelectionType(resourceTypes)
                                .label("Res. Type:")
                                .bind(resourceTypes,selectedResourceType),
                            Field.ofStringType("")
                                    .label("Res. Name:")
                                    .bind(selectedResourceName),
                            Field.ofSingleSelectionType(patternTypes)
                                .label("Pattern Type:")
                                .bind(patternTypes,selectedPatternType)
                    ),
                    Group.of(
                            Field.ofStringType("User:")
                                    .label("Principal:")
                                    .bind(selectedPrincipal),
                            Field.ofStringType("*")
                                    .label("Host:")
                                    .bind(selectedHost),
                            Field.ofSingleSelectionType(aclOperations)
                                    .label("ACL Operation:")
                                    .bind(aclOperations, seletedAclOperation),
                            Field.ofSingleSelectionType(permissionTypes)
                                    .label("Perm. Type:")
                                    .bind(permissionTypes,selectedPermissionType)
                    )
            ).title("Create ACL")
                    .binding(BindingMode.CONTINUOUS);

            Dialog<SimpleObjectProperty> dialog = new Dialog<>();
            Main.applyIcon(dialog);
            dialog.setTitle("Create ACL");

            ButtonType createAclButton = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(createAclButton, ButtonType.CANCEL);

            Node addClusterButton = dialog.getDialogPane().lookupButton(createAclButton);
            addClusterButton.getStyleClass().add("primary");

            FormRenderer formRenderer = new FormRenderer(form);
            formRenderer.setPrefWidth(500);
            dialog.getDialogPane().setContent(formRenderer);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == createAclButton) {
                    AclBinding aclBinding = new AclBinding(new ResourcePattern(selectedResourceType.get(), selectedResourceName.get(), selectedPatternType.get()),
                                                            new AccessControlEntry(selectedPrincipal.get(),selectedHost.get(),seletedAclOperation.get(),selectedPermissionType.get()));

                    return new SimpleObjectProperty<AclBinding>(aclBinding);
                }
                return null;
            });

            Optional<SimpleObjectProperty> aclBinding = dialog.showAndWait();


            if (aclBinding.isPresent()) {
                kafkaesqueAdminClient.addAcl((AclBinding) aclBinding.get().get());
            }

        } catch (Exception e) {
            ErrorAlert.show(e);
        }
    }
}
