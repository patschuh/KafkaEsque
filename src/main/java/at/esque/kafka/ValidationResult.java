package at.esque.kafka;

public class ValidationResult {

    private final boolean valid;
    private final boolean isBlank;

    private final String validationError;

    public ValidationResult(boolean valid, boolean isBlank) {
        this.valid = valid;
        this.validationError = null;
        this.isBlank = isBlank;
    }

    public ValidationResult(boolean valid, boolean isBlank, String validationError) {
        this.valid = valid;
        this.validationError = validationError;
        this.isBlank = isBlank;
    }

    public boolean isValid() {
        return valid;
    }

    public String getValidationError() {
        return validationError;
    }

    public boolean isBlank() {
        return isBlank;
    }
}
