package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.CannotProceedException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;

/**
 * KajiLibrary's javax.naming.spi.NamingManager -- the machinery that builds contexts and objects.
 *
 * <p>All static. It is the point where JNDI decides <b>who</b> serves each thing, and that is why
 * it is the point where control of everything can be taken.
 *
 * <h2>The two builders are installed only once</h2>
 *
 * <p>{@link #setInitialContextFactoryBuilder} and {@link #setObjectFactoryBuilder} fail if there
 * already was one. The uniqueness is not an implementation convenience: they are the two levers
 * that decide which provider and which factories are used in the <b>whole</b> process, and if they
 * could be replaced, the first library to use them would be at the mercy of the second.
 *
 * <h2>How an object factory is looked up</h2>
 *
 * <p>With no builder installed, {@link #getObjectInstance} follows the default path: if the data is
 * a {@link Reference} naming a factory class, <b>that</b> class is loaded and used as the factory;
 * then the classes named in the {@code java.naming.factory.object} property are tried.
 *
 * <p>Loading the class the data names is what makes references useful and at the same time a real
 * risk: the class name comes from the directory, so whoever can write there chooses which code is
 * loaded. Installing an {@link ObjectFactoryBuilder} of your own is the way to close that.
 *
 * <p>That path differs from the JDK 25 in several ways. The JDK also unwraps a
 * {@link Referenceable} into its {@code Reference}; when the reference names a factory, the JDK
 * returns that factory's answer (or {@code refInfo} if it cannot be loaded) and never goes on to
 * the property's list, while this one does; a reference without a factory has its URL addresses
 * tried first; a builder's factory answer is returned as is, even null (here null becomes
 * {@code refInfo}); and the JDK passes every factory class named by a reference through the
 * {@code jdk.jndi.object.factoriesFilter} global filter, which this library does not have.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #getInitialContext} with no builder and no {@code java.naming.factory.initial}
 * property throws {@link NoInitialContextException}, which is what it declares for "no
 * provider". With the property set it loads the class and works: the lookup is really
 * implemented. (But {@code javax.naming.InitialContext} does not call it; see its class header.)
 *
 * <p>{@link #getURLContext} always returns null. Looking up a context by URL scheme takes the
 * package convention of {@code java.naming.factory.url.pkgs} and an implementation per scheme, and
 * this library ships none. Null is what the contract defines as "no context for that scheme", so
 * the caller carries on along the normal path without noticing anything odd.
 */
public class NamingManager {

    /** The key under which a continuation context receives the exception that caused it. */
    public static final String CPE = "java.naming.spi.CannotProceedException";

    /** The installed one, or null. */
    private static ObjectFactoryBuilder objectFactoryBuilder = null;

    /** The installed one, or null. */
    private static InitialContextFactoryBuilder initialContextFactoryBuilder = null;

    /**
     * Package-private, as in the JDK: the class is static methods only. (An earlier note said
     * public for compatibility.)
     */
    NamingManager() {
    }

    /**
     * Installs the object factory builder.
     *
     * @throws IllegalStateException if there already was one; see the class note
     */
    public static synchronized void setObjectFactoryBuilder(ObjectFactoryBuilder builder)
        throws NamingException {
        if (objectFactoryBuilder != null) {
            throw new IllegalStateException("ObjectFactoryBuilder already set");
        }
        objectFactoryBuilder = builder;
    }

    /**
     * The object that corresponds to that data.
     *
     * <p>See the lookup path, and how it differs from the JDK, in the class note.
     *
     * @return the object, or {@code refInfo} as is if no factory recognized it
     */
    public static Object getObjectInstance(Object refInfo, Name name, Context nameCtx,
                                           Hashtable<?, ?> environment) throws Exception {
        ObjectFactoryBuilder builder;
        synchronized (NamingManager.class) {
            builder = objectFactoryBuilder;
        }
        if (builder != null) {
            ObjectFactory factory = builder.createObjectFactory(refInfo, environment);
            Object made = factory.getObjectInstance(refInfo, name, nameCtx, environment);
            return (made == null) ? refInfo : made;
        }
        // Default path: the class the reference itself names.
        if (refInfo instanceof Reference) {
            String className = ((Reference) refInfo).getFactoryClassName();
            if (className != null) {
                ObjectFactory factory = loadFactory(className);
                if (factory != null) {
                    Object made = factory.getObjectInstance(refInfo, name, nameCtx, environment);
                    if (made != null) {
                        return made;
                    }
                }
            }
        }
        // And then the property's ones, in order.
        String list = property(environment, Context.OBJECT_FACTORIES);
        if (list != null) {
            String[] names = list.split(":");
            int i = 0;
            while (i < names.length) {
                ObjectFactory factory = loadFactory(names[i].trim());
                if (factory != null) {
                    Object made = factory.getObjectInstance(refInfo, name, nameCtx, environment);
                    if (made != null) {
                        return made;
                    }
                }
                i = i + 1;
            }
        }
        // None recognized it: what came in is returned, which is what the contract asks for.
        return refInfo;
    }

    /**
     * The context that serves that URL scheme.
     *
     * @return null always in KajiLibrary; see the class note
     */
    public static Context getURLContext(String scheme, Hashtable<?, ?> environment)
        throws NamingException {
        return null;
    }

    /**
     * The initial context of the configured provider.
     *
     * @throws NoInitialContextException if there is no provider
     */
    public static Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        InitialContextFactoryBuilder builder;
        synchronized (NamingManager.class) {
            builder = initialContextFactoryBuilder;
        }
        if (builder != null) {
            return builder.createInitialContextFactory(environment).getInitialContext(environment);
        }
        String className = property(environment, Context.INITIAL_CONTEXT_FACTORY);
        if (className == null) {
            throw new NoInitialContextException(
                "Need to specify class name in environment or system property, or in an "
                    + "application resource file: " + Context.INITIAL_CONTEXT_FACTORY);
        }
        try {
            Class<?> found = Class.forName(className, true, contextLoader());
            Object made = found.getConstructor(new Class<?>[0]).newInstance(new Object[0]);
            if (!(made instanceof InitialContextFactory)) {
                throw new NoInitialContextException(
                    className + " is not an InitialContextFactory");
            }
            return ((InitialContextFactory) made).getInitialContext(environment);
        } catch (NamingException e) {
            throw e;
        } catch (Exception e) {
            NoInitialContextException failure = new NoInitialContextException(
                "Cannot instantiate class: " + className);
            failure.setRootCause(e);
            throw failure;
        }
    }

    /**
     * Installs the initial context factory builder.
     *
     * @throws IllegalStateException if there already was one
     */
    public static synchronized void setInitialContextFactoryBuilder(
        InitialContextFactoryBuilder builder) throws NamingException {
        if (initialContextFactoryBuilder != null) {
            throw new IllegalStateException("InitialContextFactoryBuilder already set");
        }
        initialContextFactoryBuilder = builder;
    }

    /** Whether one is already installed. */
    public static boolean hasInitialContextFactoryBuilder() {
        synchronized (NamingManager.class) {
            return initialContextFactoryBuilder != null;
        }
    }

    /**
     * The context in which to carry on an operation that was cut short.
     *
     * <p>When a context cannot keep resolving a name it throws {@link CannotProceedException} with
     * the object where it stopped; this turns it back into a context to resume from there.
     *
     * <p>The exception is passed in the environment under the {@link #CPE} key: the new context may
     * need to know where it comes from, and there is no other channel to tell it.
     */
    public static Context getContinuationContext(CannotProceedException cpe)
        throws NamingException {
        Hashtable<Object, Object> env = new Hashtable<Object, Object>();
        if (cpe.getEnvironment() != null) {
            env.putAll(cpe.getEnvironment());
        }
        env.put(CPE, cpe);
        Object obj = cpe.getResolvedObj();
        try {
            Object made = getObjectInstance(obj, cpe.getAltName(), cpe.getAltNameCtx(), env);
            if (made instanceof Context) {
                return (Context) made;
            }
        } catch (NamingException e) {
            throw e;
        } catch (Exception e) {
            // Could not continue: the original is propagated, since it is the one that explains the
            // cut.
        }
        throw cpe;
    }

    /**
     * What to store in place of that object.
     *
     * <p>It walks the factories of the {@code java.naming.factory.state} property and returns the
     * first non-null answer, or the object itself. (An earlier note said a {@link Referenceable}
     * nobody recognizes is stored as its reference; this method does not do that, and neither does
     * the JDK's -- that conversion is the provider's.)
     */
    public static Object getStateToBind(Object obj, Name name, Context nameCtx,
                                        Hashtable<?, ?> environment) throws NamingException {
        String list = property(environment, Context.STATE_FACTORIES);
        if (list != null) {
            String[] names = list.split(":");
            int i = 0;
            while (i < names.length) {
                StateFactory factory = loadStateFactory(names[i].trim());
                if (factory != null) {
                    Object made = factory.getStateToBind(obj, name, nameCtx, environment);
                    if (made != null) {
                        return made;
                    }
                }
                i = i + 1;
            }
        }
        return obj;
    }

    /** That property's value: first the environment, then the system. */
    static String property(Hashtable<?, ?> environment, String key) {
        if (environment != null) {
            Object v = environment.get(key);
            if (v instanceof String) {
                return (String) v;
            }
        }
        try {
            return System.getProperty(key);
        } catch (SecurityException e) {
            return null;
        }
    }

    /** The class loader used to look up the classes named by configuration. */
    static ClassLoader contextLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = NamingManager.class.getClassLoader();
        }
        return loader;
    }

    /** An object factory by class name, or null if it could not be made. */
    private static ObjectFactory loadFactory(String className) {
        Object made = instantiate(className);
        return (made instanceof ObjectFactory) ? (ObjectFactory) made : null;
    }

    /** A state factory by class name, or null. */
    private static StateFactory loadStateFactory(String className) {
        Object made = instantiate(className);
        return (made instanceof StateFactory) ? (StateFactory) made : null;
    }

    /**
     * Instantiates that class, or null.
     *
     * <p>It swallows the failure on purpose: a factory that does not load is not an error of the
     * operation, it is one factory fewer in the list. The contract asks to carry on with the next.
     */
    private static Object instantiate(String className) {
        try {
            Class<?> found = Class.forName(className, true, contextLoader());
            return found.getConstructor(new Class<?>[0]).newInstance(new Object[0]);
        } catch (Exception e) {
            return null;
        }
    }
}
