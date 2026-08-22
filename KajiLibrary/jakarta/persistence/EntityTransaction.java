package jakarta.persistence;

/**
 * The {@code EntityTransaction} interface of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary: the members and their generic
 * signatures come from the class file. What each one <em>means</em> is defined by
 * the Jakarta Persistence specification, not here.
 */
public interface EntityTransaction {

    /** @return as defined by the specification. */
    void begin();

    /** @return as defined by the specification. */
    void commit();

    /** @return as defined by the specification. */
    void rollback();

    /** @return as defined by the specification. */
    void setRollbackOnly();

    /** @return as defined by the specification. */
    boolean getRollbackOnly();

    /** @return as defined by the specification. */
    boolean isActive();

    /** @return as defined by the specification. */
    void setTimeout(Integer a0);

    /** @return as defined by the specification. */
    Integer getTimeout();
}
