package at.esque.kafka.exception;

public class MissingSchemaRegistryException extends Exception {
    private static final String EXCEPTION_MESSAGE_TEMPLATE = "The cluster config with identifier %s does not have a schema registry url configured!";

    public MissingSchemaRegistryException(String cluster) {
        super(String.format(EXCEPTION_MESSAGE_TEMPLATE, cluster));
    }
}
