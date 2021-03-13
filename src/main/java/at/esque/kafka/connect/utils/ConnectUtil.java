package at.esque.kafka.connect.utils;

import at.esque.kafka.alerts.ErrorAlert;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ConnectUtil {

    public final static List<String> PARAM_BLACK_LIST_EDIT =Arrays.asList("name","connector.class");
    public final static List<String> PARAM_BLACK_LIST_VIEW =Arrays.asList("name");

    public static String buildConfigString(Map<String,String> configMap, List<String> paramBlackList)
    {
        for(String param : paramBlackList)
        {
            Optional<String> nameKey = configMap.keySet().stream().filter(k -> k.equals(param)).findFirst();

            if (nameKey.isPresent())
            {
                configMap.remove(nameKey.get());
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        String map = "";
        try{
            map = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(configMap);
        } catch (JsonProcessingException e) {
            ErrorAlert.show(e);
        }

        return map;
    }

    public static Map<String, String> parseConfigMapFromJsonString(String jsonString)
    {

        ObjectMapper objectMapper = new ObjectMapper();
        try{
            Map<String, String> paramMap;

            paramMap = objectMapper.readValue(jsonString,Map.class);

            //Remove not allowed params if they got added manually
            for(String param : PARAM_BLACK_LIST_EDIT)
            {
                Optional<String> nameKey = paramMap.keySet().stream().filter(k -> k.equals(param)).findFirst();

                if (nameKey.isPresent())
                {
                    paramMap.remove(nameKey.get());
                }
            }

            return paramMap;

        } catch (JsonProcessingException e) {
            ErrorAlert.show(e);
        }

        return null;
    }
}
