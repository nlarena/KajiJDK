package jakarta.persistence;

// Thrown when an operation that requires an active transaction is invoked without one.
public class TransactionRequiredException extends PersistenceException {
    public TransactionRequiredException() { super(); }
    public TransactionRequiredException(Exception cause) { super(cause); }
    public TransactionRequiredException(String message) { super(message); }
    public TransactionRequiredException(String message, Exception cause) { super(message, cause); }
}
