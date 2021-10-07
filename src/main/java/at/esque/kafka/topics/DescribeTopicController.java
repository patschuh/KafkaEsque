package at.esque.kafka.topics;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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

    @FXML
    public Button topicNameClpt;

    public void setup(DescribeTopicWrapper describeTopicWrapper) {
        TopicDescription topicDescription = describeTopicWrapper.getTopicDescription();
        topicName.setText(topicDescription.name());
        partitions.setText("" + topicDescription.partitions().size());
        isInternal.setText("" + topicDescription.isInternal());
        partitionInfoList.getItems().addAll(topicDescription.partitions());
        showConfigsInListView(describeTopicWrapper.getConfigurations());

        topicNameClpt.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                final Clipboard clipboard = Clipboard.getSystemClipboard();
                final ClipboardContent content = new ClipboardContent();
                content.putString(topicName.getText());
                clipboard.setContent(content);
            }
        });
    }

    private void showConfigsInListView(Collection<ConfigEntry> configurations) {
        configurations.stream()
                .map(configEntry -> new TopicConfig(configEntry.name(), configEntry.value()))
                .forEach(configValueList.getItems()::add);
        configValueList.getItems().sort(Comparator.comparing(TopicConfig::getKey));
    }


}
