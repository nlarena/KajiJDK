package java.lang;

// Thrown to indicate that the requested operation is not supported. Typically raised by
// an implementation that inherits an optional method it deliberately does not provide —
// e.g. the read lock of a read/write lock, which has no conditions to wait on.
public class UnsupportedOperationException extends RuntimeException {

    public UnsupportedOperationException() {
        super();
    }

    public UnsupportedOperationException(String message) {
        super(message);
    }

    public UnsupportedOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedOperationException(Throwable cause) {
        super(cause);
    }
}
