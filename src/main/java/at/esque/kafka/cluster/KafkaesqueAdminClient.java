package at.esque.kafka.cluster;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.lag.viewer.Lag;
import at.esque.kafka.topics.DescribeTopicWrapper;
import javafx.application.Platform;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.CreateAclsResult;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeAclsResult;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class KafkaesqueAdminClient {
    private AdminClient adminClient;

    public KafkaesqueAdminClient(String bootstrapServers, Map<String, String> sslProps, Map<String, String> saslProps) {
        Properties props = new Properties();
        props.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.setProperty(AdminClientConfig.CLIENT_ID_CONFIG, String.format("kafkaesque-%s", UUID.randomUUID()));
        props.putAll(sslProps);
        props.putAll(saslProps);

        this.adminClient = AdminClient.create(props);

    }

    public Set<String> getTopics() {
        ListTopicsOptions options = new ListTopicsOptions();
        options.listInternal(true);
        ListTopicsResult result = adminClient.listTopics(options);
        try {
            return result.names().get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            Platform.runLater(() -> ErrorAlert.show(e));
        }
        return new HashSet<>();
    }

    public List<Integer> getPatitions(String topic) {
        DescribeTopicsResult result = adminClient.describeTopics(Collections.singletonList(topic));
        try {
            return result.topicNameValues().get(topic).get().partitions().stream()
                    .map(TopicPartitionInfo::partition)
                    .toList();
        } catch (Exception e) {
            ErrorAlert.show(e);
        }
        return Collections.emptyList();
    }

    public List<TopicPartition> getTopicPatitions(String topic) {
        DescribeTopicsResult result = adminClient.describeTopics(Collections.singletonList(topic));
        try {
            return result.topicNameValues().get(topic).get().partitions().stream()
                    .map(topicPartitionInfo -> new TopicPartition(topic, topicPartitionInfo.partition()))
                    .toList();
        } catch (Exception e) {
            ErrorAlert.show(e);
        }
        return Collections.emptyList();
    }


    public void deleteTopic(String name) throws ExecutionException, InterruptedException {
        DeleteTopicsResult result = adminClient.deleteTopics(Collections.singletonList(name));
        result.topicNameValues().get(name).get();
    }

    public void createTopic(String name, int partitions, short replicationFactor, Map<String, String> configs) throws ExecutionException, InterruptedException {
        NewTopic topic = new NewTopic(name, partitions, replicationFactor);
        topic.configs(configs);
        CreateTopicsResult result = adminClient.createTopics(Collections.singletonList(topic));
        result.all().get();
        if (result.all().isCompletedExceptionally()) {
            throw new RuntimeException("Exeption during Topic creation");
        }
    }

    public DescribeTopicWrapper describeTopic(String topic) {
        DescribeTopicsResult describeResult = adminClient.describeTopics(Collections.singletonList(topic));
        ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
        DescribeConfigsResult configsResult = adminClient.describeConfigs(Collections.singletonList(configResource));

        try {
            TopicDescription topicDescription = describeResult.topicNameValues().get(topic).get(10, TimeUnit.SECONDS);
            Config config = configsResult.values().get(configResource).get(10, TimeUnit.SECONDS);

            return new DescribeTopicWrapper(topicDescription, config);
        } catch (Exception e) {
            return new DescribeTopicWrapper(e);
        }
    }

    public List<Lag> getConsumerGroups() {
        ListConsumerGroupsResult result = adminClient.listConsumerGroups();
        try {
            Collection<ConsumerGroupListing> consumerGroupListings = result.all().get();
            return consumerGroupListings.stream().map(consumerGroupListing -> {
                Lag lag = new Lag();
                lag.setTitle(consumerGroupListing.groupId());
                return lag;
            }).toList();
        } catch (Exception e) {
            Platform.runLater(() -> ErrorAlert.show(e));
        }
        return Collections.emptyList();
    }

    public List<AclBinding> getACLs(ResourceType resourceType, PatternType resourcePattern, String resourceName, String principalName) {
        try {
            if ("".equals(resourceName))
                resourceName = null;
            if ("".equals(principalName))
                principalName = null;

            AclBindingFilter aclBindingFilter = new AclBindingFilter(new ResourcePatternFilter(resourceType, resourceName, resourcePattern),
                    new AccessControlEntryFilter(principalName, null, AclOperation.ANY, AclPermissionType.ANY));

            DescribeAclsResult describeAclsResult = adminClient.describeAcls(aclBindingFilter);

            return describeAclsResult.values().get().stream().toList();

        } catch (Exception e) {
            Platform.runLater(() -> ErrorAlert.show(e));
        }
        return Collections.emptyList();
    }

    public List<AclBinding> getACLsBySubstring(ResourceType resourceType, PatternType resourcePattern, String resourceName, String principalName) {
        try {
            AclBindingFilter aclBindingFilter = new AclBindingFilter(new ResourcePatternFilter(resourceType, null, resourcePattern),
                    new AccessControlEntryFilter(null, null, AclOperation.ANY, AclPermissionType.ANY));

            DescribeAclsResult describeAclsResult = adminClient.describeAcls(aclBindingFilter);

            return describeAclsResult.values().get().stream()
                    .filter(acl -> "".equals(resourceName) || acl.pattern().name().contains(resourceName))
                    .filter(acl -> "".equals(principalName) || acl.entry().principal().contains(principalName))
                    .toList();

        } catch (Exception e) {
            Platform.runLater(() -> ErrorAlert.show(e));
        }
        return Collections.emptyList();
    }


    public void deleteAcl(AclBinding aclBinding) {
        try {
            AclBindingFilter aclBindingFilter = new AclBindingFilter(new ResourcePatternFilter(aclBinding.pattern().resourceType(), aclBinding.pattern().name(), aclBinding.pattern().patternType()),
                    new AccessControlEntryFilter(aclBinding.entry().principal(), aclBinding.entry().host(), aclBinding.entry().operation(), aclBinding.entry().permissionType()));

            adminClient.deleteAcls(Collections.singletonList(aclBindingFilter));

        } catch (Exception e) {
            Platform.runLater(() -> ErrorAlert.show(e));
        }
    }

    public void addAcl(AclBinding aclBinding) {
        try {


            CreateAclsResult result = adminClient.createAcls(Collections.singletonList(aclBinding));

            result.all().get();

        } catch (Exception e) {
            Platform.runLater(() -> ErrorAlert.show(e));
        }
    }

    public ListConsumerGroupOffsetsResult listConsumerGroupOffsets(String groupId) {
        return adminClient.listConsumerGroupOffsets(groupId);
    }

    public void close() {
        adminClient.close();
    }
}
