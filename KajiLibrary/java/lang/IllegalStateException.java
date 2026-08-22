package java.lang;

// Thrown when a method is called at a time the receiver is not in the right state for it —
// adding to a full bounded queue, or starting something already started.
public class IllegalStateException extends RuntimeException {

    public IllegalStateException() {
        super();
    }

    public IllegalStateException(String message) {
        super(message);
    }

    public IllegalStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalStateException(Throwable cause) {
        super(cause);
    }
}
