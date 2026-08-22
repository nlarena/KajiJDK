package jakarta.persistence;

// Thrown by getSingleResult / getSingleResultOrNull when more than one result is found.
public class NonUniqueResultException extends PersistenceException {
    public NonUniqueResultException() { super(); }
    public NonUniqueResultException(Exception cause) { super(cause); }
    public NonUniqueResultException(String message) { super(message); }
    public NonUniqueResultException(String message, Exception cause) { super(message, cause); }
}
