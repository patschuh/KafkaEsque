package at.esque.kafka.topics;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartitionInfo;

import java.util.Collection;
import java.util.Comparator;

public class DescribeTopicController {

    @FXML
    private Label topicName;
    @FXML
    private Label partitions;
    @FXML
    private Label isInternal;
    @FXML
    public ListView<TopicConfig> configValueList;
    @FXML
    public ListView<TopicPartitionInfo> partitionInfoList;

    public void setup(DescribeTopicWrapper describeTopicWrapper) {
        TopicDescription topicDescription = describeTopicWrapper.getTopicDescription();
        topicName.setText(topicDescription.name());
        partitions.setText("" + topicDescription.partitions().size());
        isInternal.setText("" + topicDescription.isInternal());
        partitionInfoList.getItems().addAll(topicDescription.partitions());
        showConfigsInListView(describeTopicWrapper.getConfigurations());
    }

    private void showConfigsInListView(Collection<ConfigEntry> configurations) {
        configurations.stream()
                .map(configEntry -> new TopicConfig(configEntry.name(), configEntry.value()))
                .forEach(configValueList.getItems()::add);
        configValueList.getItems().sort(Comparator.comparing(TopicConfig::getKey));
    }


}
