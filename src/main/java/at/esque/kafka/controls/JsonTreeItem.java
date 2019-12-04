package at.esque.kafka.controls;

import javafx.scene.control.TreeItem;

public class JsonTreeItem extends TreeItem<String> {
    private String propertyName;
    private String propertyValue;

    public JsonTreeItem(String propertyName, String propertyValue) {
        super();
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
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
}
