package jakarta.persistence.spi;

/**
 * The {@code ProviderUtil} interface of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary: the members and their generic
 * signatures come from the class file. What each one <em>means</em> is defined by
 * the Jakarta Persistence specification, not here.
 */
public interface ProviderUtil {

    /** @return as defined by the specification. */
    LoadState isLoadedWithoutReference(Object a0, String a1);

    /** @return as defined by the specification. */
    LoadState isLoadedWithReference(Object a0, String a1);

    /** @return as defined by the specification. */
    LoadState isLoaded(Object a0);
}
