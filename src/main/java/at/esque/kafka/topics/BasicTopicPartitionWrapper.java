package at.esque.kafka.topics;

import org.apache.kafka.common.TopicPartitionInfo;

public class BasicTopicPartitionWrapper {
    private TopicPartitionInfo topicPartitionInfo;

    public BasicTopicPartitionWrapper(TopicPartitionInfo topicPartitionInfo) {
        this.topicPartitionInfo = topicPartitionInfo;
    }

    public TopicPartitionInfo getTopicPartitionInfo() {
        return topicPartitionInfo;
    }

    public void setTopicPartitionInfo(TopicPartitionInfo topicPartitionInfo) {
        this.topicPartitionInfo = topicPartitionInfo;
    }

    @Override
    public String toString() {
       return ""+topicPartitionInfo.partition();
    }

    public int getPartitionNumber(){
        return topicPartitionInfo.partition();
    }
}
