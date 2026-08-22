package jakarta.validation;

// KajiLibrary's jakarta.validation.UnexpectedTypeException.
public class UnexpectedTypeException extends ConstraintDeclarationException {

    public UnexpectedTypeException() {
    }

    public UnexpectedTypeException(String message) {
        super(message);
    }

    public UnexpectedTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnexpectedTypeException(Throwable cause) {
        super(cause);
    }
}
