package java.lang;

// KajiLibrary's java.lang.IllegalArgumentException — a method was passed an argument
// that is illegal or inappropriate.
public class IllegalArgumentException extends RuntimeException {

    public IllegalArgumentException() {
    }

    public IllegalArgumentException(String message) {
        super(message);
    }

    public IllegalArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalArgumentException(Throwable cause) {
        super(cause);
    }
}
