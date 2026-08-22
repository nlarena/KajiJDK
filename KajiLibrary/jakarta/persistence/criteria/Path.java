package jakarta.persistence.criteria;

import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.Collection;
import java.util.Map;

/**
 * The {@code Path} interface of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary: the members and their generic
 * signatures come from the class file. What each one <em>means</em> is defined by
 * the Jakarta Persistence specification, not here.
 */
public interface Path <X> extends Expression<X> {

    /** @return as defined by the specification. */
    Bindable<X> getModel();

    /** @return as defined by the specification. */
    Path<?> getParentPath();

    /** @return as defined by the specification. */
    <Y> Path<Y> get(SingularAttribute<? super X, Y> a0);

    /** @return as defined by the specification. */
    <E, C extends Collection<E>> Expression<C> get(PluralAttribute<? super X, C, E> a0);

    /** @return as defined by the specification. */
    <K, V, M extends Map<K, V>> Expression<M> get(MapAttribute<? super X, K, V> a0);

    /** @return as defined by the specification. */
    Expression<Class<? extends X>> type();

    /** @return as defined by the specification. */
    <Y> Path<Y> get(String a0);
}
