package jakarta.validation;

// KajiLibrary's jakarta.validation.NoProviderFoundException.
public class NoProviderFoundException extends ValidationException {

    public NoProviderFoundException() {
    }

    public NoProviderFoundException(String message) {
        super(message);
    }

    public NoProviderFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoProviderFoundException(Throwable cause) {
        super(cause);
    }
}
