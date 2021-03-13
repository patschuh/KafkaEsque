package at.esque.kafka.connect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationResult {

    private int errorCount;

    private Map<String, List<String>> paramErrors;

    public ValidationResult(int errorCount)
    {
        this.errorCount = errorCount;
        this.paramErrors = new HashMap<>();
    }

    public Map<String, List<String>> getErrors()
    {
        return paramErrors;
    }

    public void addParamErrors(String param, List<String> errors)
    {
        paramErrors.put(param, errors);
    }

    public int getErrorCount()
    {
        return errorCount;
    }

}
