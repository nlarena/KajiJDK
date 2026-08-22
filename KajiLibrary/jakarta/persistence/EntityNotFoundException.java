package jakarta.persistence;

// Thrown when an entity reference obtained via getReference is accessed but does not
// exist, or when refresh/lock find the entity gone.
public class EntityNotFoundException extends PersistenceException {
    public EntityNotFoundException() { super(); }
    public EntityNotFoundException(Exception cause) { super(cause); }
    public EntityNotFoundException(String message) { super(message); }
    public EntityNotFoundException(String message, Exception cause) { super(message, cause); }
}
