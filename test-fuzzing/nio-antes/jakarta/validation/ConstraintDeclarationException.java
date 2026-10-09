package jakarta.validation;

// KajiLibrary's jakarta.validation.ConstraintDeclarationException.
public class ConstraintDeclarationException extends ValidationException {

    public ConstraintDeclarationException() {
    }

    public ConstraintDeclarationException(String message) {
        super(message);
    }

    public ConstraintDeclarationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConstraintDeclarationException(Throwable cause) {
        super(cause);
    }
}
