package at.esque.kafka.acl.viewer;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourceType;

public class Acl {
    private StringProperty resourceType = new SimpleStringProperty();
    private StringProperty resourceName = new SimpleStringProperty();
    private StringProperty patternType = new SimpleStringProperty();
    private StringProperty principal = new SimpleStringProperty();
    private StringProperty operation = new SimpleStringProperty();
    private StringProperty permissionType = new SimpleStringProperty();
    private StringProperty host = new SimpleStringProperty();

    public Acl(ResourceType resourceType, String resourceName, PatternType patternType, String principal, AclOperation operation, AclPermissionType permissionType, String host)
    {
        this.resourceType.set(resourceType.toString());
        this.resourceName.set(resourceName);
        this.patternType.set(patternType.toString());
        this.principal.set(principal);
        this.operation.set(operation.toString());
        this.permissionType.set(permissionType.toString());
        this.host.set(host);
    }

    public String getResourceType() {
        return resourceType.get();
    }

    public StringProperty resourceTypeProperty() {
        return resourceType;
    }

    public String getResourceName() {
        return resourceName.get();
    }

    public StringProperty resourceNameProperty() {
        return resourceName;
    }

    public String getPatternType() {
        return patternType.get();
    }

    public StringProperty patternTypeProperty() {
        return patternType;
    }

    public String getPrincipal() {
        return principal.get();
    }

    public StringProperty principalProperty() {
        return principal;
    }

    public String getOperation() {
        return operation.get();
    }

    public StringProperty operationProperty() {
        return operation;
    }

    public String getPermissionType() {
        return permissionType.get();
    }

    public StringProperty permissionTypeProperty() {
        return permissionType;
    }

    public String getHost() {
        return host.get();
    }

    public StringProperty hostProperty() {
        return host;
    }
}


