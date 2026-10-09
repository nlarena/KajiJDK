package javax.imageio.spi;

/**
 * KajiLibrary's javax.imageio.spi.RegisterableService -- the provider wants to be told it is being
 * registered.
 *
 * <p>It is optional: a provider that does not implement it works the same. It serves two concrete
 * purposes:
 *
 * <ul>
 *   <li><b>declaring preferences</b>. When registered, a provider can call
 *       {@code ServiceRegistry.setOrdering} to say it goes before another. It is the only way for a
 *       specialized reader to win over a generic one;
 *   <li><b>releasing</b>. {@link #onDeregistration} is where whatever was opened gets closed.
 * </ul>
 *
 * <p>The same provider can be registered in several categories --reading and writing, for
 * example--, which is why both methods receive <b>which one</b>: the notices arrive once per
 * category.
 */
public interface RegisterableService {

    /**
     * It was just registered in that category.
     *
     * @param category which one; see the class note
     */
    void onRegistration(ServiceRegistry registry, Class<?> category);

    /** It was just deregistered from that category. */
    void onDeregistration(ServiceRegistry registry, Class<?> category);
}
