package at.esque.kafka.topics;

import at.esque.kafka.controls.FilterableListView;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Text;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.Uuid;

import java.util.Collection;
import java.util.Comparator;
import java.util.UUID;

public class DescribeTopicController {

    @FXML
    private Label topicName;
    @FXML
    private Label topicUuid;
    @FXML
    private Label partitions;
    @FXML
    private Label isInternal;
    @FXML
    public FilterableListView<TopicConfig> configValueList;
    @FXML
    public FilterableListView<TopicPartitionInfo> partitionInfoList;

    @FXML
    public Button topicNameClpt;
    @FXML
    public Button topicUuidClpt;

    public void setup(DescribeTopicWrapper describeTopicWrapper) {
        TopicDescription topicDescription = describeTopicWrapper.getTopicDescription();
        topicName.setText(topicDescription.name());
        topicUuid.setText(formatTopicUuid(topicDescription.topicId()));
        partitions.setText("" + topicDescription.partitions().size());
        isInternal.setText("" + topicDescription.isInternal());
        partitionInfoList.addItems(topicDescription.partitions());
        showConfigsInListView(describeTopicWrapper.getConfigurations());

        topicNameClpt.setOnAction(event -> copyToClipboard(topicName.getText()));
        topicUuidClpt.setOnAction(event -> copyToClipboard(topicUuid.getText()));
    }

    private void copyToClipboard(String value) {
        final ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private static String formatTopicUuid(Uuid topicId) {
        return new UUID(topicId.getMostSignificantBits(), topicId.getLeastSignificantBits()).toString();
    }

    private void showConfigsInListView(Collection<ConfigEntry> configurations) {
        configurations.stream()
                .map(configEntry -> new TopicConfig(configEntry.name(), configEntry.value()))
                .forEach(configValueList.getBaseList()::add);
        configValueList.setListComparator(Comparator.comparing(TopicConfig::getKey));
    }


}
