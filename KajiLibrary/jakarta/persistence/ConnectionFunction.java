package jakarta.persistence;

/**
 * The {@code ConnectionFunction} interface of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary: the members and their generic
 * signatures come from the class file. What each one <em>means</em> is defined by
 * the Jakarta Persistence specification, not here.
 */
public interface ConnectionFunction <C, T> {

    /** @return as defined by the specification. */
    T apply(C a0) throws Exception;
}
