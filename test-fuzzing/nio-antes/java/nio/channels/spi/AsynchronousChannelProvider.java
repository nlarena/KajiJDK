package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * KajiLibrary's java.nio.channels.spi.AsynchronousChannelProvider — the factory of the asynchronous
 * channels.
 *
 * <p>It is to the `Asynchronous*Channel`s what {@link SelectorProvider} is to the selectable ones,
 * with one difference: here the provider also makes the **group**, which is where the threads that
 * run the `CompletionHandler`s live. That is why the two ways of building a group are methods of its
 * and not constructors of the group.
 *
 * <h2>{@link #provider()}, which is the same mechanism as {@link SelectorProvider}'s</h2>
 *
 * <p>It searches in three tiers and keeps the first: the system property {@code
 * java.nio.channels.spi.AsynchronousChannelProvider}, then the providers declared in {@code
 * META-INF/services} through {@link ServiceLoader}, and last the platform's default implementation.
 * <strong>The third one is here</strong>, and this note used to say it was not, for want of network
 * natives in this VM: it has them, so there are blocking channels and a thread pool to build
 * asynchronous channels over --see {@code KajiAsyncChannelProvider}--. When none of the three gives
 * anything, {@link ServiceConfigurationError} is thrown, which is the error the JDK uses when the
 * search cannot be resolved. The first two tiers are whole and are what makes a provider of one's
 * own installable; the second finds nothing today because our `ServiceLoader` cannot enumerate
 * resources, and that is said in its own header.
 *
 * <p>Success is cached --`provider()` always returns the same object, as the contract requires-- and
 * failure is not, so that setting the property afterwards goes on serving.
 */
public abstract class AsynchronousChannelProvider {

    /** The key, which is at once the name of the service and that of the system property. */
    private static final String PROPERTY = "java.nio.channels.spi.AsynchronousChannelProvider";

    // A latch of its own and not the class; see the note of SelectorProvider.
    private static final Object CERROJO = new Object();

    private static AsynchronousChannelProvider found;

    protected AsynchronousChannelProvider() {
    }

    /**
     * The system provider, looked for in the three tiers of the header.
     *
     * <p>The first call searches; the following ones return the same object.
     *
     * @return the system provider
     * @throws ServiceConfigurationError if none of the tiers gives one, or if the one the property names
     *         cannot be loaded or instantiated, or is not an `AsynchronousChannelProvider`
     */
    public static AsynchronousChannelProvider provider() {
        synchronized (CERROJO) {
            if (found != null) {
                return found;
            }
            AsynchronousChannelProvider p = fromTheProperty();
            if (p == null) {
                p = deServiceLoader();
            }
            if (p == null) {
                // Tier 3: the stock provider. The previous version threw here, on the grounds that this VM
                // had no network natives. It has them --`jdk.internal.net.Net`-- and with them `SocketChannel`
                // and `ServerSocketChannel` work, so there is something to build asynchronous channels over: a
                // thread pool and those blocking channels. See `KajiAsyncChannelProvider`.
                p = new KajiAsyncChannelProvider();
            }
            found = p;
            return p;
        }
    }

    /**
     * The provider the system property names, or null if it is not set.
     *
     * <p>`getConstructor()` and not `getDeclaredConstructor()`: the constructor has to be public, as the
     * JDK demands. See the note of the matching method of {@link SelectorProvider}.
     */
    private static AsynchronousChannelProvider fromTheProperty() {
        String name;
        try {
            name = System.getProperty(PROPERTY);
        } catch (SecurityException ignorada) {
            return null;
        }
        if (name == null || name.length() == 0) {
            return null;
        }
        Class<?> cls;
        try {
            cls = Class.forName(name, false, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
            throw new ServiceConfigurationError(PROPERTY + ": provider " + name + " not found", e);
        }
        Object object;
        try {
            object = cls.getConstructor().newInstance();
        } catch (Exception e) {
            throw new ServiceConfigurationError(
                    PROPERTY + ": provider " + name + " could not be instantiated", e);
        }
        if (!(object instanceof AsynchronousChannelProvider)) {
            throw new ServiceConfigurationError(PROPERTY + ": provider " + name + " not a subtype");
        }
        return (AsynchronousChannelProvider) object;
    }

    /** The first provider declared in the classpath, or null if there is none. */
    private static AsynchronousChannelProvider deServiceLoader() {
        try {
            ServiceLoader<AsynchronousChannelProvider> sl =
                    ServiceLoader.load(AsynchronousChannelProvider.class);
            Iterator<AsynchronousChannelProvider> it = sl.iterator();
            if (it.hasNext()) {
                return it.next();
            }
        } catch (Throwable ignorada) {
            // A broken provider cannot stop the next tier from being tried.
        }
        return null;
    }

    /**
     * A group with a fixed number of threads.
     *
     * @param nThreads how many threads; fixed means that a `CompletionHandler` that blocks leaves the
     *        others unattended, which is the classic way of failing with this configuration
     */
    public abstract AsynchronousChannelGroup openAsynchronousChannelGroup(int nThreads,
            ThreadFactory threadFactory) throws IOException;

    /**
     * A group over a pool that grows.
     *
     * @param initialSize a hint about how many threads are waiting already, not a limit
     */
    public abstract AsynchronousChannelGroup openAsynchronousChannelGroup(ExecutorService executor,
            int initialSize) throws IOException;

    /** An asynchronous listening channel in `group`. */
    public abstract AsynchronousServerSocketChannel openAsynchronousServerSocketChannel(
            AsynchronousChannelGroup group) throws IOException;

    /** An asynchronous socket channel in `group`. */
    public abstract AsynchronousSocketChannel openAsynchronousSocketChannel(
            AsynchronousChannelGroup group) throws IOException;
}
