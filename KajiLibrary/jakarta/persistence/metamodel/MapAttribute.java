package jakarta.persistence.metamodel;

import java.util.Map;

/**
 * The {@code MapAttribute} interface of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary: the members and their generic
 * signatures come from the class file. What each one <em>means</em> is defined by
 * the Jakarta Persistence specification, not here.
 */
public interface MapAttribute <X, K, V> extends PluralAttribute<X, Map<K, V>, V> {

    /** @return as defined by the specification. */
    Class<K> getKeyJavaType();

    /** @return as defined by the specification. */
    Type<K> getKeyType();
}
