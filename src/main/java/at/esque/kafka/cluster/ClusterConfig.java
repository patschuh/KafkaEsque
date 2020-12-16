package at.esque.kafka.cluster;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterConfig {
    private StringProperty identifier = new SimpleStringProperty();
    private StringProperty bootStrapServers = new SimpleStringProperty();
    private StringProperty schemaRegistry = new SimpleStringProperty();
    private StringProperty schemaRegistryBasicAuthUserInfo = new SimpleStringProperty();
    private BooleanProperty sslEnabled = new SimpleBooleanProperty();
    private StringProperty keyStoreLocation = new SimpleStringProperty();
    private StringProperty keyStorePassword = new SimpleStringProperty();
    private StringProperty trustStoreLocation = new SimpleStringProperty();
    private StringProperty trustStorePassword = new SimpleStringProperty();
    private StringProperty saslSecurityProtocol = new SimpleStringProperty();
    private StringProperty saslMechanism = new SimpleStringProperty();
    private StringProperty saslJaasConfig = new SimpleStringProperty();

    public ClusterConfig(){}

    public ClusterConfig(ClusterConfig existingConfig) {
        if(existingConfig != null) {
            this.identifier = existingConfig.identifier;
            this.bootStrapServers = existingConfig.bootStrapServers;
            this.schemaRegistry = existingConfig.schemaRegistry;
            this.schemaRegistryBasicAuthUserInfo = existingConfig.schemaRegistryBasicAuthUserInfo;
            this.sslEnabled = existingConfig.sslEnabled;
            this.keyStoreLocation = existingConfig.keyStoreLocation;
            this.keyStorePassword = existingConfig.keyStorePassword;
            this.trustStoreLocation = existingConfig.trustStoreLocation;
            this.trustStorePassword = existingConfig.trustStorePassword;
            this.saslSecurityProtocol = existingConfig.saslSecurityProtocol;
            this.saslMechanism = existingConfig.saslMechanism;
            this.saslJaasConfig = existingConfig.saslJaasConfig;
        }
    }

    @JsonProperty("identifier")
    public String getIdentifier() {
        return identifier.get();
    }

    public StringProperty identifierProperty() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier.set(identifier);
    }

    @JsonProperty("bootstrapServers")
    public String getBootStrapServers() {
        return bootStrapServers.get();
    }

    public StringProperty bootStrapServersProperty() {
        return bootStrapServers;
    }

    public void setBootStrapServers(String bootStrapServers) {
        this.bootStrapServers.set(bootStrapServers);
    }

    @JsonProperty("schemaRegistry")
    public String getSchemaRegistry() {
        return schemaRegistry.get();
    }

    public StringProperty schemaRegistryProperty() {
        return schemaRegistry;
    }

    public void setSchemaRegistry(String schemaRegistry) {
        this.schemaRegistry.set(schemaRegistry);
    }

    @JsonProperty("sslEnabled")
    public boolean isSslEnabled() {
        return sslEnabled.get();
    }

    public BooleanProperty sslEnabledProperty() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled.set(sslEnabled);
    }

    @JsonProperty("keyStoreLocation")
    public String getKeyStoreLocation() {
        return keyStoreLocation.get();
    }

    public StringProperty keyStoreLocationProperty() {
        return keyStoreLocation;
    }

    public void setKeyStoreLocation(String keyStoreLocation) {
        this.keyStoreLocation.set(keyStoreLocation);
    }

    @JsonProperty("keyStorePassword")
    public String getKeyStorePassword() {
        return keyStorePassword.get();
    }

    public StringProperty keyStorePasswordProperty() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword.set(keyStorePassword);
    }

    @JsonProperty("trustStoreLocation")
    public String getTrustStoreLocation() {
        return trustStoreLocation.get();
    }

    public StringProperty trustStoreLocationProperty() {
        return trustStoreLocation;
    }

    public void setTrustStoreLocation(String trustStoreLocation) {
        this.trustStoreLocation.set(trustStoreLocation);
    }

    @JsonProperty("trustStorePassword")
    public String getTrustStorePassword() {
        return trustStorePassword.get();
    }

    public StringProperty trustStorePasswordProperty() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword.set(trustStorePassword);
    }


    @JsonProperty("saslSecurityProtocol")
    public String getSaslSecurityProtocol() { return saslSecurityProtocol.get(); }

    public StringProperty saslSecurityProtocolProperty() {
        return saslSecurityProtocol;
    }

    public void setSaslSecurityProtocol(String saslSecurityProtocol) { this.saslSecurityProtocol.set(saslSecurityProtocol); }

    @JsonProperty("saslMechanism")
    public String getSaslMechanism() {
        return saslMechanism.get();
    }

    public StringProperty saslMechanismProperty() {
        return saslMechanism;
    }

    public void setSaslMechanism(String saslMechanism) {
        this.saslMechanism.set(saslMechanism);
    }

    @JsonProperty("saslJaasConfig")
    public String getSaslJaasConfig() {
        return saslJaasConfig.get();
    }

    public StringProperty saslJaasConfigProperty() {
        return saslJaasConfig;
    }

    public void setSaslJaasConfig(String saslJaasConfig) { this.saslJaasConfig.set(saslJaasConfig); }

    @JsonProperty("schemaRegistryBasicAuthUserInfo")
    public String getSchemaRegistryBasicAuthUserInfo() {
        return schemaRegistryBasicAuthUserInfo.get();
    }

    public StringProperty schemaRegistryBasicAuthUserInfoProperty() {
        return schemaRegistryBasicAuthUserInfo;
    }

    public void setSchemaRegistryBasicAuthUserInfo(String schemaRegistryBasicAuthUserInfo) { this.schemaRegistryBasicAuthUserInfo.set(schemaRegistryBasicAuthUserInfo); }

    @JsonIgnore
    public boolean isSchemaRegistryHttps()
    {
        if(schemaRegistry.get() == null){
            return false;
        }
        return schemaRegistry.get().toLowerCase().startsWith("https:");
    }

    @Override
    public String toString(){
        return String.format("%s (%s)", getIdentifier(), getBootStrapServers());
    }

}
