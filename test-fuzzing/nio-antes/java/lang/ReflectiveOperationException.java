package java.lang;

// KajiLibrary's java.lang.ReflectiveOperationException — the common supertype (added in Java 7)
// of the checked exceptions thrown by reflection. Its point is ergonomic: before it existed,
// a reflective call site had to catch four unrelated exception types by hand.
public class ReflectiveOperationException extends Exception {

    public ReflectiveOperationException() {
    }

    public ReflectiveOperationException(String message) {
        super(message);
    }

    public ReflectiveOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReflectiveOperationException(Throwable cause) {
        super(cause);
    }
}
