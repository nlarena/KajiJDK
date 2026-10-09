package jakarta.persistence.criteria;

import jakarta.persistence.metamodel.Attribute;

/**
 * The {@code Fetch} interface of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary: the members and their generic
 * signatures come from the class file. What each one <em>means</em> is defined by
 * the Jakarta Persistence specification, not here.
 */
public interface Fetch <Z, X> extends FetchParent<Z, X> {

    /** @return as defined by the specification. */
    Attribute<? super Z, ?> getAttribute();

    /** @return as defined by the specification. */
    FetchParent<?, Z> getParent();

    /** @return as defined by the specification. */
    JoinType getJoinType();
}
