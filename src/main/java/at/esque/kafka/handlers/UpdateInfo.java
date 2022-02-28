package at.esque.kafka.handlers;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class UpdateInfo {
    private StringProperty tag = new SimpleStringProperty();
    private StringProperty releasePage = new SimpleStringProperty();

    public UpdateInfo(String tag, String releasePage) {
        this.tag.set(tag);
        this.releasePage.set(releasePage);
    }

    public String getTag() {
        return tag.get();
    }

    public StringProperty tagProperty() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag.set(tag);
    }

    public String getReleasePage() {
        return releasePage.get();
    }

    public StringProperty releasePageProperty() {
        return releasePage;
    }

    public void setReleasePage(String releasePage) {
        this.releasePage.set(releasePage);
    }
}
