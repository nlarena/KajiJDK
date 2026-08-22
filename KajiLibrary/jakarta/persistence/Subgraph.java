package jakarta.persistence;

/**
 * The {@code Subgraph} interface of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary: the members and their generic
 * signatures come from the class file. What each one <em>means</em> is defined by
 * the Jakarta Persistence specification, not here.
 */
public interface Subgraph <T> extends Graph<T> {

    /** @return as defined by the specification. */
    Class<T> getClassType();
}
