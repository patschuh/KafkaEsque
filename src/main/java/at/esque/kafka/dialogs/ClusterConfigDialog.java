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

    public static final String LABEL_IDENTIFIER = "Identifier";
    public static final String LABEL_BOOTSTRAP_SERVERS = "Bootstrap-Servers";
    public static final String LABEL_SCHEMA_REGISTRY_URL = "Schema Registry URL";
    public static final String LABEL_SCHEMA_REGISTRY_BASIC_AUTH_USER_INFO = "Schema Registry Basic Auth User Info";
    public static final String LABEL_ENABLE_SSL = "Enable SSL";
    public static final String LABEL_KEY_STORE_LOCATION = "Key Store Location";
    public static final String LABEL_KEY_STORE_PASSWORD = "Key Store Password";
    public static final String LABEL_TRUST_STORE_LOCATION = "Trust Store Location";
    public static final String LABEL_TRUST_STORE_PASSWORD = "Trust Store Password";
    public static final String LABEL_SASL_MECHANISM = "SASL Mechanism";
    public static final String LABEL_SASL_JAAS_CONFIG = "SASL JAAS Config";
    public static final String LABEL_SASL_SECURITY_PROTOCOL = "SASL Security Protocol";

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
                                .label(LABEL_IDENTIFIER)
                                .tooltip(LABEL_IDENTIFIER)
                                .placeholder(LABEL_IDENTIFIER)
                                .required("This field is required")
                                .editable(isCreatingNew)
                                .format(new  NullFormatStringConverter())
                                .bind(copy.identifierProperty()),
                        Field.ofStringType(copy.getBootStrapServers()==null?"":copy.getBootStrapServers())
                                .label(LABEL_BOOTSTRAP_SERVERS)
                                .tooltip(LABEL_BOOTSTRAP_SERVERS)
                                .placeholder(LABEL_BOOTSTRAP_SERVERS)
                                .required("This field is required")
                                .format(new  NullFormatStringConverter())
                                .bind(copy.bootStrapServersProperty())
                        ),
                Group.of(
                        Field.ofStringType(copy.getSchemaRegistry()==null?"":copy.getSchemaRegistry())
                                .label(LABEL_SCHEMA_REGISTRY_URL)
                                .tooltip(LABEL_SCHEMA_REGISTRY_URL)
                                .placeholder(LABEL_SCHEMA_REGISTRY_URL)
                                .format(new  NullFormatStringConverter())
                                .bind(copy.schemaRegistryProperty()),
                        Field.ofStringType(copy.getSchemaRegistryBasicAuthUserInfo()==null?"":copy.getSchemaRegistryBasicAuthUserInfo())
                                .label(LABEL_SCHEMA_REGISTRY_BASIC_AUTH_USER_INFO)
                                .tooltip(LABEL_SCHEMA_REGISTRY_BASIC_AUTH_USER_INFO)
                                .placeholder(LABEL_SCHEMA_REGISTRY_BASIC_AUTH_USER_INFO)
                                .format(new  NullFormatStringConverter())
                                .bind(copy.schemaRegistryBasicAuthUserInfoProperty())
                ),
                Group.of(
                        Field.ofStringType(copy.getkafkaConnectUrl()==null?"":copy.getkafkaConnectUrl())
                                .label("Kafka Connect URL")
                                .tooltip("Kafka Connect URL")
                                .placeholder("Kafka Connect URL")
                                .format(new  NullFormatStringConverter())
                                .bind(copy.kafkaConnectUrlProperty()),
                        Field.ofStringType(copy.getkafkaConnectBasicAuthUser()==null?"":copy.getkafkaConnectBasicAuthUser())
                                .label("Kafka Connect Basic Auth User")
                                .tooltip("Kafka Connect Basic Auth User")
                                .placeholder("Kafka Connect Basic Auth User")
                                .format(new  NullFormatStringConverter())
                                .bind(copy.kafkaConnectBasicAuthUserProperty()),
                        Field.ofStringType(copy.getkafkaConnectBasicAuthPassword()==null?"":copy.getkafkaConnectBasicAuthPassword())
                                .label("Kafka Connect Basic Auth Password")
                                .tooltip("Kafka Connect Basic Auth Password")
                                .placeholder("Kafka Connect Basic Auth Password")
                                .format(new  NullFormatStringConverter())
                                .bind(copy.kafkaConnectBasicAuthPasswordProperty())
                ),
                Group.of(
                        Field.ofBooleanType(copy.isSslEnabled())
                                .label(LABEL_ENABLE_SSL)
                                .tooltip(LABEL_ENABLE_SSL)
                                .bind(copy.sslEnabledProperty()),
                        Field.ofStringType(copy.getKeyStoreLocation()==null?"":copy.getKeyStoreLocation())
                                .label(LABEL_KEY_STORE_LOCATION)
                                .tooltip(LABEL_KEY_STORE_LOCATION)
                                .placeholder(LABEL_KEY_STORE_LOCATION)
                                .format(new  NullFormatStringConverter())
                                .bind(copy.keyStoreLocationProperty()),
                        Field.ofPasswordType(copy.getKeyStorePassword()==null?"":copy.getKeyStorePassword())
                                .label(LABEL_KEY_STORE_PASSWORD)
                                .tooltip(LABEL_KEY_STORE_PASSWORD)
                                .placeholder(LABEL_KEY_STORE_PASSWORD)
                                .format(new  NullFormatStringConverter())
                                .bind(copy.keyStorePasswordProperty()),
                        Field.ofStringType(copy.getTrustStoreLocation()==null?"":copy.getTrustStoreLocation())
                                .label(LABEL_TRUST_STORE_LOCATION)
                                .tooltip(LABEL_TRUST_STORE_LOCATION)
                                .placeholder(LABEL_TRUST_STORE_LOCATION)
                                .format(new  NullFormatStringConverter())
                                .bind(copy.trustStoreLocationProperty()),
                        Field.ofPasswordType(copy.getTrustStorePassword()==null?"":copy.getTrustStorePassword())
                                .label(LABEL_TRUST_STORE_PASSWORD)
                                .tooltip(LABEL_TRUST_STORE_PASSWORD)
                                .placeholder(LABEL_TRUST_STORE_PASSWORD)
                                .format(new  NullFormatStringConverter())
                                .bind(copy.trustStorePasswordProperty())
                ),
                Group.of(
                        Field.ofStringType(copy.getSaslSecurityProtocol()==null?"":copy.getSaslSecurityProtocol())
                                .label(LABEL_SASL_SECURITY_PROTOCOL)
                                .tooltip(LABEL_SASL_SECURITY_PROTOCOL)
                                .placeholder(LABEL_SASL_SECURITY_PROTOCOL)
                                .format(new NullFormatStringConverter())
                                .bind(copy.saslSecurityProtocolProperty()),
                        Field.ofStringType(copy.getSaslMechanism()==null?"":copy.getSaslMechanism())
                                .label(LABEL_SASL_MECHANISM)
                                .tooltip(LABEL_SASL_MECHANISM)
                                .placeholder(LABEL_SASL_MECHANISM)
                                .format(new  NullFormatStringConverter())
                                .bind(copy.saslMechanismProperty()),
                        Field.ofStringType(copy.getSaslJaasConfig()==null?"":copy.getSaslJaasConfig())
                                .label(LABEL_SASL_JAAS_CONFIG)
                                .tooltip(LABEL_SASL_JAAS_CONFIG)
                                .placeholder(LABEL_SASL_JAAS_CONFIG)
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
