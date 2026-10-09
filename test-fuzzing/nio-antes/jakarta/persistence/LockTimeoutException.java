package jakarta.persistence;

// A locking failure carrying the offending entity/object (when the provider can supply it).
public class LockTimeoutException extends PersistenceException {
    private final Object object;
    public LockTimeoutException() { super(); this.object = null; }
    public LockTimeoutException(String message) { super(message); this.object = null; }
    public LockTimeoutException(String message, Throwable cause) { super(message, cause); this.object = null; }
    public LockTimeoutException(Throwable cause) { super(cause); this.object = null; }
    public LockTimeoutException(Object object) { this.object = object; }
    public LockTimeoutException(String message, Throwable cause, Object object) { super(message, cause); this.object = object; }
    public Object getObject() { return object; }
}
