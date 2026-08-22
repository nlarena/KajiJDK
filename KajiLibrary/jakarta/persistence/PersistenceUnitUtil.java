package jakarta.persistence;

import jakarta.persistence.metamodel.Attribute;

/**
 * The {@code PersistenceUnitUtil} interface of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary: the members and their generic
 * signatures come from the class file. What each one <em>means</em> is defined by
 * the Jakarta Persistence specification, not here.
 */
public interface PersistenceUnitUtil extends PersistenceUtil {

    /** @return as defined by the specification. */
    boolean isLoaded(Object a0, String a1);

    /** @return as defined by the specification. */
    <E> boolean isLoaded(E a0, Attribute<? super E, ?> a1);

    /** @return as defined by the specification. */
    boolean isLoaded(Object a0);

    /** @return as defined by the specification. */
    void load(Object a0, String a1);

    /** @return as defined by the specification. */
    <E> void load(E a0, Attribute<? super E, ?> a1);

    /** @return as defined by the specification. */
    void load(Object a0);

    /** @return as defined by the specification. */
    boolean isInstance(Object a0, Class<?> a1);

    /** @return as defined by the specification. */
    <T> Class<? extends T> getClass(T a0);

    /** @return as defined by the specification. */
    Object getIdentifier(Object a0);

    /** @return as defined by the specification. */
    Object getVersion(Object a0);
}
