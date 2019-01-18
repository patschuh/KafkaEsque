package at.esque.kafka;

import com.google.inject.Singleton;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public final class BackGroundTaskHolder {

    private BooleanProperty isInProgress = new SimpleBooleanProperty();
    private StringProperty backGroundTaskDescription = new SimpleStringProperty();

    private StringProperty progressMessage = new SimpleStringProperty();
    private Exception exceptionInBackgroundTask;

    private AtomicBoolean stopBackGroundTask = new AtomicBoolean(false);

    public BackGroundTaskHolder() {
    }

    public boolean isIsInProgress() {
        return isInProgress.get();
    }

    public BooleanProperty isInProgressProperty() {
        return isInProgress;
    }

    public void setIsInProgress(boolean isInProgress) {
        this.isInProgress.set(isInProgress);
    }

    public String getBackGroundTaskDescription() {
        return backGroundTaskDescription.get();
    }

    public StringProperty backGroundTaskDescriptionProperty() {
        return backGroundTaskDescription;
    }

    public void setBackGroundTaskDescription(String backGroundTaskDescription) {
        this.backGroundTaskDescription.set(backGroundTaskDescription);
    }

    public StringProperty progressMessageProperty() {
        return progressMessage;
    }

    public void setProgressMessage(String message) {
        this.progressMessage.set(message);
    }

    public boolean getStopBackGroundTask() {
        return stopBackGroundTask.get();
    }

    public void setStopBackGroundTask(boolean stopBackGroundTask) {
        this.stopBackGroundTask.set(stopBackGroundTask);
    }

    public void backgroundTaskStopped() {
        stopBackGroundTask.set(false);
        isInProgress.set(false);
    }
}
