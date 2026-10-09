package jakarta.persistence.metamodel;

/**
 * The {@code SingularAttribute} interface of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary: the members and their generic
 * signatures come from the class file. What each one <em>means</em> is defined by
 * the Jakarta Persistence specification, not here.
 */
public interface SingularAttribute <X, T> extends Attribute<X, T>, Bindable<T> {

    /** @return as defined by the specification. */
    boolean isId();

    /** @return as defined by the specification. */
    boolean isVersion();

    /** @return as defined by the specification. */
    boolean isOptional();

    /** @return as defined by the specification. */
    Type<T> getType();
}
