package at.esque.kafka.cluster;

import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ClusterConfig {
    private StringProperty identifier = new SimpleStringProperty();
    private StringProperty bootStrapServers = new SimpleStringProperty();
    private StringProperty schemaRegistry = new SimpleStringProperty();
    private BooleanProperty sslEnabled = new SimpleBooleanProperty();
    private StringProperty keyStoreLocation = new SimpleStringProperty();
    private StringProperty keyStorePassword = new SimpleStringProperty();
    private StringProperty trustStoreLocation = new SimpleStringProperty();
    private StringProperty trustStorePassword = new SimpleStringProperty();

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

    @Override
    public String toString(){
        return String.format("%s (%s)", getIdentifier(), getBootStrapServers());
    }
}
