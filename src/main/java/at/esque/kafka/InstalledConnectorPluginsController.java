package at.esque.kafka;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.cluster.ClusterConfig;
import at.esque.kafka.connect.KafkaesqueConnectClient;
import at.esque.kafka.controls.FilterableListView;
import at.esque.kafka.handlers.ConfigHandler;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Window;


public class InstalledConnectorPluginsController {

    @FXML
    private FilterableListView<String> connectorPluginListView;

    private KafkaesqueConnectClient kafkaesqueConnectClient;

    public void setup(ClusterConfig selectedConfig, ConfigHandler configHandler) {
        kafkaesqueConnectClient = new KafkaesqueConnectClient(selectedConfig.getkafkaConnectUrl(), selectedConfig.getkafkaConnectBasicAuthUser(), selectedConfig.getkafkaConnectBasicAuthPassword(), selectedConfig.isKafkaConnectuseSsl(), configHandler.getSslProperties(selectedConfig));
        connectorPluginListView.setListComparator(String::compareTo);

        refreshConnectorPlugins(null);

    }

    public void refreshConnectorPlugins(ActionEvent actionEvent) {
        try {
            connectorPluginListView.setItems(FXCollections.observableArrayList(kafkaesqueConnectClient.getInstalledConnectorPlugins()));
        } catch (Exception e) {
            ErrorAlert.show(e, getWindow());
        }
    }

    private Window getWindow() {
        return connectorPluginListView.getScene().getWindow();
    }

}
