package jakarta.validation;

// KajiLibrary's jakarta.validation.ConstraintDefinitionException.
public class ConstraintDefinitionException extends ValidationException {

    public ConstraintDefinitionException() {
    }

    public ConstraintDefinitionException(String message) {
        super(message);
    }

    public ConstraintDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConstraintDefinitionException(Throwable cause) {
        super(cause);
    }
}
