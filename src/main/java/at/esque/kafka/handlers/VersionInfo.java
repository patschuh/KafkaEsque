package at.esque.kafka.handlers;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.gradle.util.VersionNumber;

public class VersionInfo {
    private StringProperty name = new SimpleStringProperty();
    private StringProperty version = new SimpleStringProperty();
    private StringProperty revision = new SimpleStringProperty();
    private StringProperty tag = new SimpleStringProperty();
    private StringProperty buildTime = new SimpleStringProperty();
    private StringProperty buildJvm = new SimpleStringProperty();


    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getVersion() {
        return version.get();
    }

    public StringProperty versionProperty() {
        return version;
    }

    public void setVersion(String version) {
        this.version.set(version);
    }

    public String getRevision() {
        return revision.get();
    }

    public StringProperty revisionProperty() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision.set(revision);
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

    public String getBuildTime() {
        return buildTime.get();
    }

    public StringProperty buildTimeProperty() {
        return buildTime;
    }

    public void setBuildTime(String buildTime) {
        this.buildTime.set(buildTime);
    }

    public String getBuildJvm() {
        return buildJvm.get();
    }

    public StringProperty buildJvmProperty() {
        return buildJvm;
    }

    public void setBuildJvm(String buildJvm) {
        this.buildJvm.set(buildJvm);
    }

    public VersionNumber releaseVersion(){
        if(tag.get() == null){
            return null;
        }
        return VersionNumber.parse(tag.get().substring(1));
    }
}
