package jakarta.persistence.metamodel;

/**
 * The {@code EntityType} interface of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary: the members and their generic
 * signatures come from the class file. What each one <em>means</em> is defined by
 * the Jakarta Persistence specification, not here.
 */
public interface EntityType <X> extends IdentifiableType<X>, Bindable<X> {

    /** @return as defined by the specification. */
    String getName();
}
