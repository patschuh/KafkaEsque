package at.esque.kafka.connect;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Status {
    private String type;
    private String status;
    private String workerId;
    private List<TaskStatus> taskStatusList= new ArrayList();

    public Status(String type, String status, String workerId)
    {
        this.type = type;
        this.status = status;
        this.workerId = workerId;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getWorkerId() {
        return workerId;
    }

    public List<TaskStatus> getTaskStatusList() {
        return taskStatusList;
    }

    public void addTaskStatus(int id, String status, String workerId, String trace)
    {
        taskStatusList.add(new TaskStatus(id,status,workerId,trace));
    }

    public static class TaskStatus {
        private IntegerProperty id = new SimpleIntegerProperty();
        private StringProperty status = new SimpleStringProperty();
        private StringProperty workerId = new SimpleStringProperty();
        private StringProperty trace = new SimpleStringProperty();

        public TaskStatus(int id, String status, String workerId, String trace)
        {
            this.id.set(id);
            this.status.setValue(status);
            this.workerId.setValue(workerId);
            this.trace.setValue(trace);
        }

        public int getId() {
            return id.get();
        }

        public String getStatus() {
            return status.getValue();
        }

        public String getWorkerId() {
            return workerId.getValue();
        }

        public String getTrace() {
            return trace.getValue();
        }

        public IntegerProperty idProperty() { return id; }
        public StringProperty statusProperty() {
            return status;
        }
        public StringProperty workerIdProperty() { return workerId; }
        public StringProperty traceProperty() {
            return trace;
        }

    }
}
