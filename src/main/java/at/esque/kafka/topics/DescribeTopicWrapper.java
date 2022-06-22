package at.esque.kafka.topics;

import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;

import java.util.Collection;

public class DescribeTopicWrapper {
    private TopicDescription topicDescription;
    private Config configurations;
    private Exception exception;

    public DescribeTopicWrapper(TopicDescription topicDescription, Config configurations) {
        this.topicDescription = topicDescription;
        this.configurations = configurations;
    }

    public DescribeTopicWrapper(Exception e) {
        this.exception = e;
    }

    public Collection<ConfigEntry> getConfigurations() {
        return configurations.entries();
    }

    public TopicDescription getTopicDescription() {
        return topicDescription;
    }

    public void setTopicDescription(TopicDescription topicDescription) {
        this.topicDescription = topicDescription;
    }

    public void setConfigurations(Config configurations) {
        this.configurations = configurations;
    }

    public Exception getException() {
        return exception;
    }

    public boolean isFailed() {
        return exception != null;
    }
}
