package java.util;

// Thrown when a collection is structurally modified while something else is iterating it.
// It is a *fail-fast* signal, not a guarantee: detecting the interference cheaply and blaming
// it immediately beats letting the iterator return nonsense much later.
public class ConcurrentModificationException extends RuntimeException {

    public ConcurrentModificationException() {
        super();
    }

    public ConcurrentModificationException(String message) {
        super(message);
    }

    public ConcurrentModificationException(Throwable cause) {
        super(cause);
    }

    public ConcurrentModificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
