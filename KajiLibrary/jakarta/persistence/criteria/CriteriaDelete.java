package jakarta.persistence.criteria;

import jakarta.persistence.metamodel.EntityType;

/**
 * The {@code CriteriaDelete} interface of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary: the members and their generic
 * signatures come from the class file. What each one <em>means</em> is defined by
 * the Jakarta Persistence specification, not here.
 */
public interface CriteriaDelete <T> extends CommonAbstractCriteria {

    /** @return as defined by the specification. */
    Root<T> from(Class<T> a0);

    /** @return as defined by the specification. */
    Root<T> from(EntityType<T> a0);

    /** @return as defined by the specification. */
    Root<T> getRoot();

    /** @return as defined by the specification. */
    CriteriaDelete<T> where(Expression<Boolean> a0);

    /** @return as defined by the specification. */
    CriteriaDelete<T> where(Predicate... a0);
}
