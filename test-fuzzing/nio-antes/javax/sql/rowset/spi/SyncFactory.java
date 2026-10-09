package javax.sql.rowset.spi;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * The registry of synchronization providers: where a {@code RowSet} gets its own.
 *
 * <h2>All static, and why</h2>
 *
 * <p>Because the registry belongs to the process. That there is a {@link #getSyncFactory()}
 * returning an instance is historical —the class was conceived as a factory with state and ended up
 * being a global table— and none of the instance methods exists. It is kept because it is API.
 *
 * <h2>Where the providers come from</h2>
 *
 * <p>From four places, and the order matters because the last one to register an identifier wins:
 *
 * <ol>
 *   <li>the default provider, which is always there;
 *   <li>the system property {@link #ROWSET_SYNC_PROVIDER};
 *   <li>the declared services {@link ServiceLoader} finds;
 *   <li>whatever somebody registers by hand with {@link #registerProvider}.
 * </ol>
 *
 * <p>The idea is that an application can change the provider without touching code —with a
 * property— and that a library can contribute its own just by being on the classpath.
 *
 * <h2>An identifier is a class name</h2>
 *
 * <p>There is no table from names to implementations: the identifier <strong>is</strong> the fully
 * qualified name of the class, and {@link #getInstance} loads it by reflection. It is what allows
 * registering a provider without this class knowing it.
 *
 * <p>The consequence is that registering validates nothing: an identifier is accepted when it is
 * registered and only fails when the instance is asked for, if the class is not there or is not a
 * {@link SyncProvider}.
 *
 * @since 1.5
 */
public class SyncFactory {

    /** The system property with the provider's class name. */
    public static final String ROWSET_SYNC_PROVIDER = "rowset.provider.classname";

    /** The system property with the vendor's name. */
    public static final String ROWSET_SYNC_VENDOR = "rowset.provider.vendor";

    /** The system property with the provider's version. */
    public static final String ROWSET_SYNC_PROVIDER_VERSION = "rowset.provider.version";

    /**
     * The JDK's default provider.
     *
     * <p>This library does not come with it, so asking for it fails with {@link
     * SyncFactoryException} saying the class is not there. The name is kept because it is the one
     * the specification names and the one a ported application is going to ask for.
     */
    private static final String DEFAULT_PROVIDER = "com.sun.rowset.providers.RIOptimisticProvider";

    /** Provider identifier to class name; today they are the same, see the class note. */
    private static final Map<String, String> registered = new TreeMap<String, String>();

    private static final SyncFactory INSTANCE = new SyncFactory();

    private static Logger logger;
    private static Context jndiContext;
    private static boolean initialized;

    private SyncFactory() {
    }

    /**
     * Loads the providers from the three automatic sources, only once.
     *
     * <p>It is lazy and not a static initializer: walking the {@link ServiceLoader} loads
     * third-party classes, and that should not happen just because somebody names this class.
     */
    private static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        registered.put(DEFAULT_PROVIDER, DEFAULT_PROVIDER);

        final String fromSystem = System.getProperty(ROWSET_SYNC_PROVIDER);
        if (fromSystem != null && fromSystem.length() > 0) {
            registered.put(fromSystem, fromSystem);
        }

        try {
            for (final SyncProvider p : ServiceLoader.load(SyncProvider.class)) {
                registered.put(p.getProviderID(), p.getClass().getName());
            }
        } catch (final java.util.ServiceConfigurationError e) {
            // A badly declared service cannot bring down the ones that are fine: it is noted and
            // the rest carry on.
            if (logger != null) {
                logger.log(Level.WARNING, "a declared provider could not be loaded", e);
            }
        }
    }

    /**
     * Registers a provider by its identifier, which is the fully qualified name of its class.
     *
     * <p>It does not validate: the class is looked for only in {@link #getInstance}.
     *
     * @param providerID the identifier
     * @throws SyncFactoryException if the identifier is {@code null} or empty (JDK 25 accepts an
     *     empty one)
     */
    public static synchronized void registerProvider(final String providerID)
            throws SyncFactoryException {
        if (providerID == null || providerID.length() == 0) {
            throw new SyncFactoryException("the provider identifier cannot be empty");
        }
        initialize();
        registered.put(providerID, providerID);
    }

    /**
     * The factory's instance.
     *
     * <p>It is good for nothing: all the useful methods are static. It exists because the API
     * declares it.
     *
     * @return the instance
     */
    public static SyncFactory getSyncFactory() {
        return INSTANCE;
    }

    /**
     * Removes a provider from the registry.
     *
     * @param providerID the identifier
     * @throws SyncFactoryException if the identifier is {@code null} or empty
     */
    public static synchronized void unregisterProvider(final String providerID)
            throws SyncFactoryException {
        if (providerID == null || providerID.length() == 0) {
            throw new SyncFactoryException("the provider identifier cannot be empty");
        }
        initialize();
        registered.remove(providerID);
    }

    /**
     * A new instance of the provider with that identifier.
     *
     * <p>New and not shared: a provider has state —the lock level, for example— and two different
     * {@code RowSet}s should not overwrite each other's.
     *
     * @param providerID the identifier
     * @return the provider
     * @throws SyncFactoryException if the identifier is empty, the class is not there, it is not a
     *     {@link SyncProvider}, or it could not be instantiated
     */
    public static SyncProvider getInstance(final String providerID) throws SyncFactoryException {
        if (providerID == null || providerID.length() == 0) {
            throw new SyncFactoryException("the provider identifier cannot be empty");
        }
        initialize();
        final String className;
        synchronized (SyncFactory.class) {
            final String r = registered.get(providerID);
            className = r != null ? r : providerID;
        }
        try {
            final Class<?> c = Class.forName(className, true,
                    Thread.currentThread().getContextClassLoader());
            final Object o = c.getDeclaredConstructor().newInstance();
            if (!(o instanceof SyncProvider)) {
                throw new SyncFactoryException(className + " is not a SyncProvider");
            }
            return (SyncProvider) o;
        } catch (final SyncFactoryException e) {
            throw e;
        } catch (final ClassNotFoundException e) {
            final SyncFactoryException s =
                    new SyncFactoryException("provider class not found: " + className);
            s.initCause(e);
            throw s;
        } catch (final ReflectiveOperationException e) {
            final SyncFactoryException s =
                    new SyncFactoryException("could not instantiate the provider " + className);
            s.initCause(e);
            throw s;
        }
    }

    /**
     * The registered providers, already instantiated.
     *
     * <p>The ones that cannot be instantiated are skipped instead of making the whole enumeration
     * fail: a broken provider should not hide the ones that work.
     *
     * @return the enumeration
     * @throws SyncFactoryException if the registry could not be read
     */
    public static Enumeration<SyncProvider> getRegisteredProviders()
            throws SyncFactoryException {
        initialize();
        final Vector<SyncProvider> out = new Vector<SyncProvider>();
        final String[] ids;
        synchronized (SyncFactory.class) {
            ids = registered.keySet().toArray(new String[registered.size()]);
        }
        for (int i = 0; i < ids.length; i++) {
            try {
                out.add(getInstance(ids[i]));
            } catch (final SyncFactoryException e) {
                if (logger != null) {
                    logger.log(Level.FINE, "provider not instantiable: " + ids[i], e);
                }
            }
        }
        return out.elements();
    }

    /**
     * Sets the logger the factory leaves its trace through.
     *
     * @param logger the logger
     * @throws NullPointerException if it is {@code null}
     */
    public static void setLogger(final Logger logger) {
        if (logger == null) {
            throw new NullPointerException("the logger cannot be null");
        }
        SyncFactory.logger = logger;
    }

    /**
     * Sets the logger and its level.
     *
     * @param logger the logger
     * @param level the level
     * @throws NullPointerException if the logger is {@code null}
     */
    public static void setLogger(final Logger logger, final Level level) {
        if (logger == null) {
            throw new NullPointerException("the logger cannot be null");
        }
        logger.setLevel(level);
        SyncFactory.logger = logger;
    }

    /**
     * The logger that was set.
     *
     * <p>It fails if none was set, instead of returning a default one: whoever asks for the logger
     * wants the one they configured, and returning another would make their messages come out
     * somewhere they do not expect.
     *
     * @return the logger
     * @throws SyncFactoryException if none was set
     */
    public static Logger getLogger() throws SyncFactoryException {
        final Logger l = logger;
        if (l == null) {
            throw new SyncFactoryException("(SyncFactory) : No logger has been set");
        }
        return l;
    }

    /**
     * Sets a JNDI context to read providers registered in the directory from.
     *
     * <p>It is for an application server, which publishes its providers in the JNDI tree instead of
     * in a system property. What is looked for are {@link SyncProvider} objects; the rest of
     * whatever there is in the context is ignored.
     *
     * @param ctx the context
     * @throws SyncFactoryException if it is {@code null} or could not be walked
     */
    public static synchronized void setJNDIContext(final Context ctx) throws SyncFactoryException {
        if (ctx == null) {
            throw new SyncFactoryException("the JNDI context cannot be null");
        }
        initialize();
        jndiContext = ctx;
        try {
            final Hashtable<String, SyncProvider> found =
                    new Hashtable<String, SyncProvider>();
            collect(ctx, found);
            for (final Map.Entry<String, SyncProvider> e : found.entrySet()) {
                registered.put(e.getKey(), e.getValue().getClass().getName());
            }
        } catch (final NamingException e) {
            final SyncFactoryException s =
                    new SyncFactoryException("could not read the JNDI context");
            s.initCause(e);
            throw s;
        }
    }

    private static void collect(final Context ctx, final Map<String, SyncProvider> out)
            throws NamingException {
        final NamingEnumeration<javax.naming.Binding> e = ctx.listBindings("");
        while (e.hasMore()) {
            final javax.naming.Binding b = e.next();
            final Object o = b.getObject();
            if (o instanceof Context) {
                collect((Context) o, out);
            } else if (o instanceof SyncProvider) {
                final SyncProvider p = (SyncProvider) o;
                out.put(p.getProviderID(), p);
            }
        }
    }
}
