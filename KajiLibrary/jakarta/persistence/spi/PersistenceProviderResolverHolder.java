package jakarta.persistence.spi;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the current {@link PersistenceProviderResolver}.
 *
 * <p>The public surface matches the specification. The body does NOT: the reference
 * implementation discovers providers with {@code java.util.ServiceLoader}, caches them per
 * class loader behind {@code WeakReference}s, and logs through {@code java.util.logging} —
 * none of which KajiLibrary has. Since we ship no persistence provider either (JPA's engine
 * is an ORM plus JDBC plus a database, deliberately out of scope), discovery would have
 * nothing to find: the default resolver here answers with an empty list, which is the honest
 * result rather than a pretend one.
 *
 * <p>A caller can still install its own resolver through
 * {@link #setPersistenceProviderResolver}, which is the whole point of this indirection, so
 * the class is functional for the case that matters.
 */
public class PersistenceProviderResolverHolder {

    private static PersistenceProviderResolver resolver = new NoProviderResolver();

    public static PersistenceProviderResolver getPersistenceProviderResolver() {
        return resolver;
    }

    public static void setPersistenceProviderResolver(PersistenceProviderResolver newResolver) {
        // Passing null restores the default, as the specification requires.
        PersistenceProviderResolver next = newResolver;
        if (next == null) {
            next = new NoProviderResolver();
        }
        resolver = next;
    }
}

/**
 * The stand-in default: no providers, nothing cached. Package-private and top-level rather
 * than nested, because a nested class is what finding #101 trips over.
 */
class NoProviderResolver implements PersistenceProviderResolver {

    public List<PersistenceProvider> getPersistenceProviders() {
        return new ArrayList<PersistenceProvider>();
    }

    public void clearCachedProviders() {
        // Nothing is cached, so there is nothing to clear.
    }
}
