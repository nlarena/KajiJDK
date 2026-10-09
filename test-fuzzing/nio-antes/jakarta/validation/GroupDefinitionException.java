package jakarta.validation;

// KajiLibrary's jakarta.validation.GroupDefinitionException.
public class GroupDefinitionException extends ValidationException {

    public GroupDefinitionException() {
    }

    public GroupDefinitionException(String message) {
        super(message);
    }

    public GroupDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    public GroupDefinitionException(Throwable cause) {
        super(cause);
    }
}
