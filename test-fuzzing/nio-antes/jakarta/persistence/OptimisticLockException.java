package jakarta.persistence;

// A locking failure carrying the offending entity/object (when the provider can supply it).
public class OptimisticLockException extends PersistenceException {
    private final Object entity;
    public OptimisticLockException() { super(); this.entity = null; }
    public OptimisticLockException(String message) { super(message); this.entity = null; }
    public OptimisticLockException(String message, Throwable cause) { super(message, cause); this.entity = null; }
    public OptimisticLockException(Throwable cause) { super(cause); this.entity = null; }
    public OptimisticLockException(Object entity) { this.entity = entity; }
    public OptimisticLockException(String message, Throwable cause, Object entity) { super(message, cause); this.entity = entity; }
    public Object getEntity() { return entity; }
}
