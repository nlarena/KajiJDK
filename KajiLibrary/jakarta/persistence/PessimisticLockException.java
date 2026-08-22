package jakarta.persistence;

// A locking failure carrying the offending entity/object (when the provider can supply it).
public class PessimisticLockException extends PersistenceException {
    private final Object entity;
    public PessimisticLockException() { super(); this.entity = null; }
    public PessimisticLockException(String message) { super(message); this.entity = null; }
    public PessimisticLockException(String message, Throwable cause) { super(message, cause); this.entity = null; }
    public PessimisticLockException(Throwable cause) { super(cause); this.entity = null; }
    public PessimisticLockException(Object entity) { this.entity = entity; }
    public PessimisticLockException(String message, Throwable cause, Object entity) { super(message, cause); this.entity = entity; }
    public Object getEntity() { return entity; }
}
