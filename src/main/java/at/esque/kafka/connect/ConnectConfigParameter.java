package at.esque.kafka.connect;

import javafx.beans.property.*;
import org.apache.kafka.common.acl.AclBinding;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPluginConfigValidationResults;

import java.util.ArrayList;
import java.util.Collection;

public class ConnectConfigParameter {

    private ConnectorPluginConfigValidationResults.Config config;
    private StringProperty name = new SimpleStringProperty();
    private StringProperty value = new SimpleStringProperty();
    private StringProperty type = new SimpleStringProperty();
    private BooleanProperty required = new SimpleBooleanProperty();
    private StringProperty defaultValue = new SimpleStringProperty();
    private StringProperty importance = new SimpleStringProperty();
    private StringProperty documentation = new SimpleStringProperty();
    private StringProperty group = new SimpleStringProperty();
    private StringProperty width = new SimpleStringProperty();
    private StringProperty displayName = new SimpleStringProperty();
    private IntegerProperty order = new SimpleIntegerProperty();


    public ConnectConfigParameter(ConnectorPluginConfigValidationResults.Config config) {
        this.config = config;
        this.name.set(config.getDefinition().getName());
        this.value.set(config.getValue().getValue());
        this.type.set(config.getDefinition().getType());
        this.required.set(config.getDefinition().isRequired());
        this.defaultValue.set(config.getDefinition().getDefaultValue());
        this.importance.set(config.getDefinition().getImportance());
        this.documentation.set(config.getDefinition().getDocumentation());
        this.group.set(config.getDefinition().getGroup());
        this.width.set(config.getDefinition().getWidth());
        this.displayName.set(config.getDefinition().getDisplayName());
        this.order.set(config.getDefinition().getOrder());
    }

    public String getName() {
        return name.get();
    }
    public StringProperty nameProperty() {
        return name;
    }

    public String getValue() {
        return value.get();
    }
    public StringProperty valueProperty() {
        return value;
    }

    public String getType() {
        return type.get();
    }
    public StringProperty typeProperty() {
        return type;
    }

    public boolean getRequired() {
        return required.get();
    }
    public BooleanProperty requiredProperty() {
        return required;
    }

    public String getDefaultValue() {
        return defaultValue.get();
    }
    public StringProperty defaultValueProperty() {
        return defaultValue;
    }

    public String getImportance() {
        return importance.get();
    }
    public StringProperty importanceProperty() {
        return importance;
    }

    public String getDocumentation() {return documentation.get();}
    public StringProperty documentationProperty() {return documentation;}

    public String getGroup() {
        return group.get();
    }
    public StringProperty groupProperty() {
        return group;
    }

    public String getWidth() {
        return width.get();
    }
    public StringProperty widthProperty() {
        return width;
    }

    public String getDiplayName() {
        return displayName.get();
    }
    public StringProperty displayNameProperty() {
        return displayName;
    }

    public int getOrder() {return order.get();}
    public IntegerProperty OrderProperty() {
        return order;
    }

    public ConnectorPluginConfigValidationResults.Config getConfig() {
        return config;
    }
}
