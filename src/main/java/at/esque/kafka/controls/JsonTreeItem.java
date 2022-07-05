package at.esque.kafka.controls;

import javafx.scene.control.TreeItem;

public class JsonTreeItem extends TreeItem<String> {
    private String propertyName;
    private String propertyValue;
    private String propertyChangedType;

    public JsonTreeItem(String propertyName, String propertyValue) {
        this(propertyName, propertyValue, null);
    }

    public JsonTreeItem(String propertyName, String propertyValue, String propertyChangedType) {
        super();
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
        this.propertyChangedType = propertyChangedType;
        updateValue();
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
        updateValue();
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
        updateValue();
    }

    public String getPropertyChangedType() {
        return propertyChangedType;
    }

    public void setPropertyChangedType(String propertyChangedType) {
        this.propertyChangedType = propertyChangedType;
    }

    private void updateValue() {
        if (propertyName == null && propertyValue == null) {
            this.setValue("<null>");
        } else if (propertyName != null && propertyValue == null) {
            this.setValue(propertyName);
        } else if (propertyName == null && propertyValue != null) {
            this.setValue(propertyValue);
        } else {
            this.setValue(propertyName + ": " + propertyValue);
        }
    }

    public String getPath(){
        return getPath(this);
    }

    private String getPath(JsonTreeItem jsonTreeItem){
        if(jsonTreeItem.getParent() == null){
            return "";
        }
        return getPath((JsonTreeItem) jsonTreeItem.getParent()) + "/"+jsonTreeItem.getPropertyName();
    }
}
