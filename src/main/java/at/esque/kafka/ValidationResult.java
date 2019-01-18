package at.esque.kafka;

public class ValidationResult {

    private final boolean valid;

    private final String validationError;

    public ValidationResult(boolean valid) {
        this.valid = valid;
        this.validationError = null;
    }

    public ValidationResult(boolean valid, String validationError) {
        this.valid = valid;
        this.validationError = validationError;
    }

    public boolean isValid() {
        return valid;
    }

    public String getValidationError() {
        return validationError;
    }
}
