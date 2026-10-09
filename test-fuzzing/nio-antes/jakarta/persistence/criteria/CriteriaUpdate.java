package jakarta.persistence.criteria;

import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * The {@code CriteriaUpdate} interface of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary: the members and their generic
 * signatures come from the class file. What each one <em>means</em> is defined by
 * the Jakarta Persistence specification, not here.
 */
public interface CriteriaUpdate <T> extends CommonAbstractCriteria {

    /** @return as defined by the specification. */
    Root<T> from(Class<T> a0);

    /** @return as defined by the specification. */
    Root<T> from(EntityType<T> a0);

    /** @return as defined by the specification. */
    Root<T> getRoot();

    /** @return as defined by the specification. */
    <Y, X extends Y> CriteriaUpdate<T> set(SingularAttribute<? super T, Y> a0, X a1);

    /** @return as defined by the specification. */
    <Y> CriteriaUpdate<T> set(SingularAttribute<? super T, Y> a0, Expression<? extends Y> a1);

    /** @return as defined by the specification. */
    <Y, X extends Y> CriteriaUpdate<T> set(Path<Y> a0, X a1);

    /** @return as defined by the specification. */
    <Y> CriteriaUpdate<T> set(Path<Y> a0, Expression<? extends Y> a1);

    /** @return as defined by the specification. */
    CriteriaUpdate<T> set(String a0, Object a1);

    /** @return as defined by the specification. */
    CriteriaUpdate<T> where(Expression<Boolean> a0);

    /** @return as defined by the specification. */
    CriteriaUpdate<T> where(Predicate... a0);
}
