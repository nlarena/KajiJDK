package jakarta.persistence;

/**
 * The {@code Cache} interface of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary: the members and their generic
 * signatures come from the class file. What each one <em>means</em> is defined by
 * the Jakarta Persistence specification, not here.
 */
public interface Cache {

    /** @return as defined by the specification. */
    boolean contains(Class<?> a0, Object a1);

    /** @return as defined by the specification. */
    void evict(Class<?> a0, Object a1);

    /** @return as defined by the specification. */
    void evict(Class<?> a0);

    /** @return as defined by the specification. */
    void evictAll();

    /** @return as defined by the specification. */
    <T> T unwrap(Class<T> a0);
}
