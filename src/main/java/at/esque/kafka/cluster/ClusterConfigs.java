package at.esque.kafka.cluster;

import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;

public class ClusterConfigs {
    private ObservableList<ClusterConfig> clusterConfigs = FXCollections.observableArrayList();

    @Transient
    public  ObservableList<ClusterConfig> getClusterConfigs() {
        return clusterConfigs;
    }

    public void setClusterConfigs( ObservableList<ClusterConfig> clusterConfigs) {
        this.clusterConfigs = clusterConfigs;
    }

    @JsonProperty("clusterConfigs")
    public List<ClusterConfig> getClusterConfigAsList(){
        return new ArrayList<>(clusterConfigs);
    }

    @JsonProperty("clusterConfigs")
    public void setClusterConfigAsList(List<ClusterConfig> clusterConfigs){
      this.clusterConfigs = FXCollections.observableArrayList(clusterConfigs);
    }

}
