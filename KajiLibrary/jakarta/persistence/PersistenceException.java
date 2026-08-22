package jakarta.persistence;

// The root of the Jakarta Persistence exception hierarchy. Unchecked (extends
// RuntimeException); a provider throws it (or a subclass) when a persistence operation
// fails. All but the "…LockException" subtypes marking the transaction for rollback are
// otherwise unspecified as to rollback.
public class PersistenceException extends RuntimeException {

    public PersistenceException() {
        super();
    }

    public PersistenceException(String message) {
        super(message);
    }

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public PersistenceException(Throwable cause) {
        super(cause);
    }
}
