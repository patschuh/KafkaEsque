package at.esque.kafka;

public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
