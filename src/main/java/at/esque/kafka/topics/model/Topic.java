package at.esque.kafka.topics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class Topic {
    @JsonProperty("name")
    private String name;
    @JsonProperty("partitions")
    private int partitions;
    @JsonProperty("replication-factor")
    private short replicationFactor;
    @JsonProperty("configs")
    private Map<String, String> configs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPartitions() {
        return partitions;
    }

    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }

    public short getReplacationFactor() {
        return replicationFactor;
    }

    public void setReplacationFactor(short replacationFactor) {
        this.replicationFactor = replacationFactor;
    }

    public Map<String, String> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, String> configs) {
        this.configs = configs;
    }
}
