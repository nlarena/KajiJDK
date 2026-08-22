package jakarta.persistence;

/**
 * The {@code ConnectionConsumer} interface of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary: the members and their generic
 * signatures come from the class file. What each one <em>means</em> is defined by
 * the Jakarta Persistence specification, not here.
 */
public interface ConnectionConsumer <C> {

    /** @return as defined by the specification. */
    void accept(C a0) throws Exception;
}
