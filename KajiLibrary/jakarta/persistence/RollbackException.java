package jakarta.persistence;

// Thrown when a transaction commit fails and the transaction is rolled back.
public class RollbackException extends PersistenceException {
    public RollbackException() { super(); }
    public RollbackException(String message) { super(message); }
    public RollbackException(String message, Throwable cause) { super(message, cause); }
    public RollbackException(Throwable cause) { super(cause); }
}
