package jakarta.persistence.criteria;

import jakarta.persistence.metamodel.Attribute;

/**
 * The {@code Join} interface of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary: the members and their generic
 * signatures come from the class file. What each one <em>means</em> is defined by
 * the Jakarta Persistence specification, not here.
 */
public interface Join <Z, X> extends From<Z, X> {

    /** @return as defined by the specification. */
    Join<Z, X> on(Expression<Boolean> a0);

    /** @return as defined by the specification. */
    Join<Z, X> on(Predicate... a0);

    /** @return as defined by the specification. */
    Predicate getOn();

    /** @return as defined by the specification. */
    Attribute<? super Z, ?> getAttribute();

    /** @return as defined by the specification. */
    From<?, Z> getParent();

    /** @return as defined by the specification. */
    JoinType getJoinType();
}
