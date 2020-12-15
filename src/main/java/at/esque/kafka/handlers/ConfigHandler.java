package at.esque.kafka.handlers;

import at.esque.kafka.alerts.ErrorAlert;
import at.esque.kafka.cluster.ClusterConfig;
import at.esque.kafka.cluster.ClusterConfigs;
import at.esque.kafka.cluster.TopicMessageTypeConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.inject.Singleton;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class ConfigHandler {
    private static final String CONFIG_DIRECTORY = System.getProperty("user.home") + "/.kafkaesque/%s";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigHandler.class);

    private ObjectMapper objectMapper = new ObjectMapper();
    private YAMLMapper yamlMapper = new YAMLMapper();

    private File clusterConfig;

    private ClusterConfigs clusterConfigs;

    private Map<String, Map<String, TopicMessageTypeConfig>> cachedConfigs = new ConcurrentHashMap<>();

    private Map<String, String> settings;

    public ConfigHandler() {
    }

    public TopicMessageTypeConfig getConfigForTopic(String clusterIdentification, String topic) {
        Map<String, TopicMessageTypeConfig> configs = getTopicConfigForClusterIdentifier(clusterIdentification);
        TopicMessageTypeConfig topicMessageTypeConfig = configs.get(topic);
        if (topicMessageTypeConfig == null) {
            topicMessageTypeConfig = new TopicMessageTypeConfig(topic);
            configs.put(topic, topicMessageTypeConfig);
        }
        return topicMessageTypeConfig;
    }

    public Map<String, TopicMessageTypeConfig> getTopicConfigForClusterIdentifier(String clusterIdentification) {
        Map<String, TopicMessageTypeConfig> config = cachedConfigs.get(clusterIdentification);
        if (config != null) {
            return config;
        }
        File configFile = new File(String.format(CONFIG_DIRECTORY, clusterIdentification + "/topics.yaml"));
        if (!configFile.exists()) {
            List<TopicMessageTypeConfig> topicConfigs = new ArrayList<>();
            configFile.getParentFile().mkdirs();
            try {
                yamlMapper.writeValue(configFile, topicConfigs);
            } catch (IOException e) {
                ErrorAlert.show(e);
            }
        }
        try {
            List<TopicMessageTypeConfig> topicConfigList = yamlMapper.readValue(configFile, new TypeReference<List<TopicMessageTypeConfig>>() {
            });
            Map<String, TopicMessageTypeConfig> configMap = topicConfigList.stream()
                    .collect(Collectors.toMap(TopicMessageTypeConfig::getName, Function.identity()));
            cachedConfigs.put(clusterIdentification, configMap);
            return configMap;
        } catch (IOException e) {
            ErrorAlert.show(e);
        }
        cachedConfigs.put(clusterIdentification, new HashMap<>());
        return cachedConfigs.get(clusterIdentification);
    }

    public Map<String, String> getSettingsProperties() {
        if (settings != null) {
            return settings;
        }
        File configFile = new File(String.format(CONFIG_DIRECTORY, "settings.yaml"));
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            settings = new HashMap<>();
            settings.put(Settings.USE_SYSTEM_MENU_BAR, "true");
            try {
                yamlMapper.writeValue(configFile, settings);
            } catch (IOException e) {
                ErrorAlert.show(e);
            }
        }
        try {
            settings = yamlMapper.readValue(configFile, new TypeReference<Map<String, String>>() {
            });
        } catch (IOException e) {
            ErrorAlert.show(e);
        }
        return settings;
    }

    public Map<String, String> readConsumerConfigs(String clusterIdentification) throws IOException {
        return readConfigsMap(clusterIdentification, "consumer");
    }

    public Map<String, String> readProducerConfigs(String clusterIdentification) throws IOException {
        return readConfigsMap(clusterIdentification, "producer");
    }

    public Map<String, String> readConfigsMap(String clusterIdentification, String fileNameWithoutExtension) throws IOException {
        File configFile = new File(String.format(CONFIG_DIRECTORY, clusterIdentification + "/" + fileNameWithoutExtension + ".yaml"));
        configFile.getParentFile().mkdirs();

        if (configFile.exists()) {
            return yamlMapper.readValue(configFile, new TypeReference<Map<String, String>>() {
            });
        }
        return new HashMap<>();
    }

    public void writeConfigsMap(String clusterIdentification, String fileNameWithoutExtension, Map<String, String> configMap) throws IOException {
        File configFile = new File(String.format(CONFIG_DIRECTORY, clusterIdentification + "/" + fileNameWithoutExtension + ".yaml"));
        configFile.getParentFile().mkdirs();

        yamlMapper.writeValue(configFile, configMap);
    }


    public ClusterConfigs loadOrCreateConfigs() {
        clusterConfig = new File(String.format(CONFIG_DIRECTORY, "clusters.json"));
        if (clusterConfigs != null) {
            return clusterConfigs;
        } else if (clusterConfig.exists()) {
            try {
                clusterConfigs = objectMapper.readValue(clusterConfig, ClusterConfigs.class);
                return clusterConfigs;
            } catch (IOException e) {
                ErrorAlert.show(e);
            }
        } else {
            clusterConfigs = new ClusterConfigs();
            try {
                clusterConfig.getParentFile().mkdirs();
                objectMapper.writeValue(clusterConfig, clusterConfigs);
                return clusterConfigs;
            } catch (IOException e) {
                ErrorAlert.show(e);
            }
        }
        clusterConfigs = new ClusterConfigs();
        return clusterConfigs;
    }

    public void saveConfigs() {
        try {
            objectMapper.writeValue(clusterConfig, clusterConfigs);
        } catch (IOException e) {
            ErrorAlert.show(e);
        }
    }

    public void saveTopicMessageTypeConfigs(String clusterIdentification) {
        File configFile = new File(String.format(CONFIG_DIRECTORY, clusterIdentification + "/topics.yaml"));
        try {
            yamlMapper.writeValue(configFile, cachedConfigs.get(clusterIdentification).values());
        } catch (IOException e) {
            ErrorAlert.show(e);
        }
    }

    public Map<String, String> getSslProperties(ClusterConfig config) {
        Map<String, String> props = new HashMap<>();
        if (config.isSslEnabled()) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        }

        if (config.isSchemaRegistryHttps()) {
            props.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        }

        if (StringUtils.isNotEmpty(config.getKeyStoreLocation())) {
            String keyStoreLocation = getJksStoreLocation(config.getIdentifier(), config.getKeyStoreLocation());
            if (keyStoreLocation != null) {
                props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keyStoreLocation);
                props.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keyStoreLocation);

                if (StringUtils.isNotEmpty(config.getKeyStorePassword())) {
                    props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, config.getKeyStorePassword());
                    props.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, config.getKeyStorePassword());
                }
            }
        }
        if (StringUtils.isNotEmpty(config.getTrustStoreLocation())) {
            String trustStoreLocation = getJksStoreLocation(config.getIdentifier(), config.getTrustStoreLocation());

            if (trustStoreLocation != null) {
                props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStoreLocation);
                props.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStoreLocation);

                if (StringUtils.isNotEmpty(config.getTrustStorePassword())) {
                    props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, config.getTrustStorePassword());
                    props.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, config.getTrustStorePassword());
                }
            }
        }
        return props;
    }


    public Map<String, String> getSaslProperties(ClusterConfig config) {
        Map<String, String> props = new HashMap<>();

        if (StringUtils.isNoneEmpty(config.getSaslSecurityProtocol()))
        {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,config.getSaslSecurityProtocol());
        }

        if (StringUtils.isNotEmpty(config.getSaslMechanism()))
        {
            props.put(SaslConfigs.SASL_MECHANISM,config.getSaslMechanism());
        }


        if (StringUtils.isNotEmpty(config.getSaslJaasConfig()))
        {
            props.put(SaslConfigs.SASL_JAAS_CONFIG,config.getSaslJaasConfig());
        }

        return props;
    }

    public Map<String, ?> getSchemaRegistryAuthProperties(ClusterConfig config) {
        Map<String, String> props = new HashMap<>();

        if (StringUtils.isNoneEmpty(config.getSchemaRegistryBasicAuthUserInfo()))
        {
            props.put(SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE,"USER_INFO");
            props.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SchemaRegistryClientConfig.USER_INFO_CONFIG, config.getSchemaRegistryBasicAuthUserInfo());
        }

        return props;
    }

    private String getJksStoreLocation(String clusterIdentification, String location) {
        File jksStore = new File(location);
        if (jksStore.exists() && jksStore.isFile()) {
            return location;
        }
        jksStore = new File(String.format(CONFIG_DIRECTORY, clusterIdentification), location);
        if (jksStore.exists() && jksStore.isFile()) {
            return jksStore.getAbsolutePath();
        }
        return null;
    }
}
