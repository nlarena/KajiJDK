package java.net.spi;

/**
 * KajiLibrary's java.net.spi.InetAddressResolverProvider -- where the resolver comes from.
 *
 * <p>It is loaded as a service and the platform takes <b>one only</b>: name resolution is global to
 * the process and it would make no sense for two parts of the program to see the Internet
 * differently. That is why there is {@link #name()}, which serves to be able to say which one ended
 * up in place when something does not resolve as expected.
 *
 * <p>{@link Configuration#builtinResolver()} is the important piece of the design: it hands the
 * provider the system's resolver, so the normal thing is not to replace resolution but to <b>wrap
 * it</b> -- resolve a few names of one's own and delegate the rest. Without that, any provider would
 * have to reimplement the whole of DNS in order to intercept one name.
 */
public abstract class InetAddressResolverProvider {

    /** For the subclasses. */
    protected InetAddressResolverProvider() {
    }

    /**
     * This provider's resolver.
     *
     * @param configuration what the platform lends it; see the class's note about wrapping
     */
    public abstract InetAddressResolver get(Configuration configuration);

    /** A name to identify it in diagnostics. */
    public abstract String name();

    /** What the platform gives the provider when it asks it for the resolver. */
    public interface Configuration {

        /** The system's resolver, to delegate whatever one does not want to handle. */
        InetAddressResolver builtinResolver();

        /**
         * This machine's local name.
         *
         * <p>It goes here --and is not looked up-- because finding it out usually needs resolving,
         * and the provider does not exist yet while it is being built.
         */
        String lookupLocalHostName();
    }
}
