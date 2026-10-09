package jakarta.persistence;

// Thrown by getSingleResult when there is no result.
public class NoResultException extends PersistenceException {
    public NoResultException() { super(); }
    public NoResultException(String message) { super(message); }
}
