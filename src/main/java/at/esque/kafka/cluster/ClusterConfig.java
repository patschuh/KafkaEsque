package at.esque.kafka.cluster;

import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ClusterConfig {
    private StringProperty identifier = new SimpleStringProperty();
    private StringProperty bootStrapServers = new SimpleStringProperty();
    private StringProperty schemaRegistry = new SimpleStringProperty();

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

    @Override
    public String toString(){
        return String.format("%s (%s)", getIdentifier(), getBootStrapServers());
    }
}
