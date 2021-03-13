package at.esque.kafka.connect;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.config.SslConfigs;
import org.eclipse.jetty.util.StringUtil;
import org.sourcelab.kafka.connect.apiclient.Configuration;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;
import org.sourcelab.kafka.connect.apiclient.request.dto.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class KafkaesqueConnectClient {
    private KafkaConnectClient connectClient;


    public KafkaesqueConnectClient(String kafkaConnectURL, String kafkaConnectBasicAuthUser, String kafkaConnectBasicAuthPassword, Map<String, String> sslProps) {

        Configuration configuration = new Configuration(kafkaConnectURL);

        if(!StringUtil.isEmpty(kafkaConnectBasicAuthUser) && !StringUtils.isEmpty(kafkaConnectBasicAuthPassword)) {
            configuration.useBasicAuth(kafkaConnectBasicAuthUser, kafkaConnectBasicAuthPassword);
        }

        if (sslProps.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG) != null)
        {
            configuration.useTrustStore(new File(sslProps.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG)), sslProps.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG));
        }

        if (sslProps.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG) != null)
        {
            configuration.useTrustStore(new File(sslProps.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG)), sslProps.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG));
        }
        if (sslProps.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG) != null)
        {
            configuration.useKeyStore(new File(sslProps.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG)), sslProps.get(SslConfigs.SSL_KEY_PASSWORD_CONFIG));
        }

        connectClient = new KafkaConnectClient(configuration);
    }

    public List<String> getConnectors()
    {
        return connectClient.getConnectors().stream().collect(Collectors.toList());
    }

    public Map<String, String> getConnectorConfig(String connectorName)
    {
        return connectClient.getConnectorConfig(connectorName);
    }

    public List<String> getInstalledConnectorPlugins()
    {
        List<String> installedPluginClasses = new ArrayList<>();
        Collection<ConnectorPlugin> installedPlugins = connectClient.getConnectorPlugins();
        installedPluginClasses = installedPlugins.stream().map(ConnectorPlugin::getClassName).collect(Collectors.toList());

        return installedPluginClasses;
    }

    public boolean deleteConnector(String connectorName)
    {
        return connectClient.deleteConnector(connectorName);
    }

    public Status getConnectorStatus(String connectorName)
    {
        ConnectorStatus status = connectClient.getConnectorStatus(connectorName);

        Status conStatus = new Status(status.getType(), status.getConnector().get("state"), status.getConnector().get("worker_id"));

        for(ConnectorStatus.TaskStatus taskStatus : status.getTasks())
        {
            conStatus.addTaskStatus(taskStatus.getId(),taskStatus.getState(),taskStatus.getWorkerId(), taskStatus.getTrace());
        }

        return conStatus;
    }

    public boolean pauseConnector(String connectorName)
    {
       return connectClient.pauseConnector(connectorName);
    }

    public boolean resumeConnector(String connectorName)
    {
       return connectClient.resumeConnector(connectorName);
    }

    public boolean restartConnector(String connectorName)
    {
        return connectClient.restartConnector(connectorName);
    }

    public boolean restartConnectorTask(String connenctorName, int taskId)
    {
        return connectClient.restartConnectorTask(connenctorName,taskId);
    }

    public List<ConnectConfigParameter> getConnectorConfigParameters(String connectorClass)
    {
        ConnectorPluginConfigDefinition connectDef = ConnectorPluginConfigDefinition.newBuilder()
                .withName(connectorClass)
                .withConfig("connector.class",connectorClass)
                .withConfig("tasks.max", 1)
                .withConfig("topics", "test")
                .build();

        ConnectorPluginConfigValidationResults configValidationResults = connectClient.validateConnectorPluginConfig(connectDef);

        Collection<ConnectorPluginConfigValidationResults.Config> paramList = configValidationResults.getConfigs();

        List<ConnectConfigParameter> mappedparamList = new ArrayList<>();

        paramList.stream().forEach(c -> mappedparamList.add(new ConnectConfigParameter(c)));

        return mappedparamList;

    }

    public ValidationResult validateConnectorConfig(String name, String connectorClass,  Map<String,String> params)
    {
        ConnectorPluginConfigDefinition connectDef = ConnectorPluginConfigDefinition.newBuilder()
                .withName(connectorClass)
                .withConfig(params)
                .withConfig("connector.class",connectorClass)
                .withConfig("name",name)
                .build();

        ConnectorPluginConfigValidationResults configValidationResults = connectClient.validateConnectorPluginConfig(connectDef);

        ValidationResult valRes = new ValidationResult(configValidationResults.getErrorCount());

        for(ConnectorPluginConfigValidationResults.Config c : configValidationResults.getConfigs())
        {
            Collection<String>  errors = c.getValue().getErrors();

            if(errors.size() > 0)
            {
                valRes.addParamErrors(c.getDefinition().getName(),new ArrayList<>(errors));
            }
        }

      return valRes;
    }

    public void createConnector(String name, String connectorClass, Map<String,String> params)
    {
        NewConnectorDefinition connectDef = NewConnectorDefinition.newBuilder()
                .withName(name)
                .withConfig(params)
                .withConfig("connector.class",connectorClass)
                .build();

        connectClient.addConnector(connectDef);

    }

    public void updateConnector(String name, String connectorClass, Map<String,String> params)
    {
        params.put("connector.class", connectorClass);

        connectClient.updateConnectorConfig(name,params);

    }
}
