package at.esque.kafka.dialogs;

import at.esque.kafka.Main;
import at.esque.kafka.cluster.ClusterConfig;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Group;
import com.dlsc.formsfx.model.util.BindingMode;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.util.StringConverter;

import java.util.Optional;

public class ClusterConfigDialog {
    private ClusterConfigDialog(){}

    public static Optional<ClusterConfig> show() {
        return show(null);
    }

    public static Optional<ClusterConfig> show(ClusterConfig existingConfig) {
        boolean isCreatingNew = existingConfig == null;

        ClusterConfig copy = new ClusterConfig(existingConfig);

        Form form = Form.of(
                Group.of(
                        Field.ofStringType(copy.getIdentifier()==null?"":copy.getIdentifier())
                                .label("Identifier")
                                .tooltip("Identifier")
                                .placeholder("Identifier")
                                .required("This field is required")
                                .editable(isCreatingNew)
                                .format(new  NullFormatStringConverter())
                                .bind(copy.identifierProperty()),
                        Field.ofStringType(copy.getBootStrapServers()==null?"":copy.getBootStrapServers())
                                .label("Bootstrap-Servers")
                                .tooltip("Bootstrap-Servers")
                                .placeholder("Bootstrap-Servers")
                                .required("This field is required")
                                .format(new  NullFormatStringConverter())
                                .bind(copy.bootStrapServersProperty())
                        ),
                Group.of(
                        Field.ofStringType(copy.getSchemaRegistry()==null?"":copy.getSchemaRegistry())
                                .label("Schema Registry URL")
                                .tooltip("Schema Registry URL")
                                .placeholder("Schema Registry URL")
                                .format(new  NullFormatStringConverter())
                                .bind(copy.schemaRegistryProperty()),
                        Field.ofStringType(copy.getSchemaRegistryBasicAuthUserInfo()==null?"":copy.getSchemaRegistryBasicAuthUserInfo())
                                .label("Schema Registry Basic Auth User Info")
                                .tooltip("Schema Registry Basic Auth User Info")
                                .placeholder("Schema Registry Basic Auth User Info")
                                .format(new  NullFormatStringConverter())
                                .bind(copy.schemaRegistryBasicAuthUserInfoProperty())
                ),
                Group.of(
                        Field.ofBooleanType(copy.isSslEnabled())
                                .label("Enable SSL")
                                .tooltip("Enable SSL"),
                        Field.ofStringType(copy.getKeyStoreLocation()==null?"":copy.getKeyStoreLocation())
                                .label("Key Store Location")
                                .tooltip("Key Store Location")
                                .placeholder("Key Store Location")
                                .format(new  NullFormatStringConverter())
                                .bind(copy.keyStoreLocationProperty()),
                        Field.ofStringType(copy.getKeyStorePassword()==null?"":copy.getKeyStorePassword())
                                .label("Key Store Password")
                                .tooltip("Key Store Password")
                                .placeholder("Key Store Password")
                                .format(new  NullFormatStringConverter())
                                .bind(copy.keyStorePasswordProperty()),
                        Field.ofStringType(copy.getTrustStoreLocation()==null?"":copy.getTrustStoreLocation())
                                .label("Trust Store Location")
                                .tooltip("Trust Store Location")
                                .placeholder("Trust Store Location")
                                .format(new  NullFormatStringConverter())
                                .bind(copy.trustStoreLocationProperty()),
                        Field.ofStringType(copy.getTrustStorePassword()==null?"":copy.getTrustStorePassword())
                                .label("Trust Store Password")
                                .tooltip("Trust Store Password")
                                .placeholder("Trust Store Password")
                                .format(new  NullFormatStringConverter())
                                .bind(copy.trustStorePasswordProperty())
                ),
                Group.of(
                        Field.ofStringType(copy.getSaslMechanism()==null?"":copy.getSaslMechanism())
                                .label("SASL Mechanism")
                                .tooltip("SASL Mechanism")
                                .placeholder("SASL Mechanism")
                                .format(new  NullFormatStringConverter())
                                .bind(copy.saslMechanismProperty()),
                        Field.ofStringType(copy.getSaslJaasConfig()==null?"":copy.getSaslJaasConfig())
                                .label("SASL JAAS Config")
                                .tooltip("SASL JAAS Config")
                                .placeholder("SASL JAAS Config")
                                .format(new  NullFormatStringConverter())
                                .bind(copy.saslJaasConfigProperty())
                )
        ).title(isCreatingNew ? "Add new Kafka Cluster" : "Change Kafka Cluster")
                .binding(BindingMode.CONTINUOUS);

        Dialog<ClusterConfig> dialog = new Dialog<>();
        Main.applyIcon(dialog);
        dialog.setTitle(isCreatingNew ? "Add new Kafka Cluster" : "Change Kafka Cluster");

        ButtonType addClusterButtonType = new ButtonType(existingConfig != null ? "Change" : "Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addClusterButtonType, ButtonType.CANCEL);


        Node addClusterButton = dialog.getDialogPane().lookupButton(addClusterButtonType);
        addClusterButton.getStyleClass().add("primary");

        addClusterButton.disableProperty().bind(form.validProperty().not());

        FormRenderer formRenderer = new FormRenderer(form);
        formRenderer.setPrefWidth(1000);
        dialog.getDialogPane().setContent(formRenderer);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addClusterButtonType) {
                return copy;
            }
            return null;
        });

        return dialog.showAndWait();

    }

    private static class NullFormatStringConverter extends StringConverter<String>{
        public NullFormatStringConverter() {
        }

        @Override
        public String toString(String s) {
            if(s == null || "null".equals(s)){
                return "";
            }
            return s;
        }

        @Override
        public String fromString(String s) {
            if("".equals(s) || "null".equals(s)){
                return null;
            }
            return s;
        }
    }
}
