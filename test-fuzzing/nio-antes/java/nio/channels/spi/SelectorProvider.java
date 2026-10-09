package java.nio.channels.spi;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * KajiLibrary's java.nio.channels.spi.SelectorProvider — the factory of selectable channels.
 *
 * <p>Every network channel and every selector is born of a provider. The indirection exists so that
 * the whole implementation of `java.nio.channels` --epoll, kqueue, IOCP, or a toy one for tests--
 * can be changed without touching a line of the code that uses it.
 *
 * <h2>{@link #provider()} and the three tiers of the contract</h2>
 *
 * <p>The static one looks for the system provider and keeps the first one it finds:
 *
 * <ol>
 *   <li>the system property {@code java.nio.channels.spi.SelectorProvider}, taken as the full name
 *       of a class with a no-argument constructor;
 *   <li>the first provider declared through {@link ServiceLoader}, that is,
 *       {@code META-INF/services/java.nio.channels.spi.SelectorProvider} in the classpath;
 *   <li>the platform's default implementation.
 * </ol>
 *
 * <p><strong>The third tier is here</strong>, and this note used to say it did not exist and could
 * not: the VM had no network natives, so there was no system `openSelector()` to return, and
 * {@link #provider()} ended in {@link ServiceConfigurationError} --which is **exactly** the error
 * the JDK uses when the search for the provider cannot be resolved, not a stub--. The VM grew its
 * network seam and `poll`, and with them `KajiSelectorProvider`, which is the default
 * implementation this tier returns.
 *
 * <p>The first two tiers **are** whole, and they are the reason this method is worth having instead
 * of being missing: they are the mechanism by which a provider of one's own is installed --over
 * memory, over a false socket, over whatever-- and they are the only way of reaching it through the
 * standard idiom. Without `provider()` the `spi` package cannot do the one thing it exists for.
 * Tier 2 finds nothing today, and not because of a shortcut here: this library's
 * {@link ServiceLoader} cannot enumerate {@code META-INF/services} because our `ClassLoader` has no
 * resources. The machinery is plugged in where it goes, so the day resources exist that tier starts
 * finding providers without a line being touched.
 *
 * <p>The result is **cached**, as the contract requires --"the first invocation locates the
 * provider"--: `provider()` always returns the same object. The failure, on the other hand, is not
 * cached: a search that found nothing is not a decision, it is the absence of configuration, and
 * whoever sets the property afterwards has to be able to reach their provider.
 *
 * <h2>The three overloads with `ProtocolFamily`</h2>
 *
 * <p>{@link #openDatagramChannel(ProtocolFamily)} is abstract, that is, a declaration and nothing
 * else. The other two are concrete and throw {@link UnsupportedOperationException}, which **is the
 * body the JDK gives them**: a provider that does not support the family asked for inherits that
 * behaviour as it is, and one that does support it overrides. Nothing is omitted here.
 */
public abstract class SelectorProvider {

    /** The key, which is at once the name of the service and that of the system property. */
    private static final String KEY = "java.nio.channels.spi.SelectorProvider";

    // A latch of its own and not the class: synchronising on `SelectorProvider.class` lets anybody
    // who has the literal block the search for the provider from outside.
    private static final Object LATCH = new Object();

    // The provider found already. It is only written on success: see the note of the header.
    private static SelectorProvider found;

    protected SelectorProvider() {
    }

    /**
     * The system provider, looked for in the three tiers of the header.
     *
     * <p>The first call searches; the following ones return the same object.
     *
     * @return the system provider
     * @throws ServiceConfigurationError if none of the tiers gives one, or if the one the property
     *         names cannot be loaded or instantiated, or is not a `SelectorProvider`
     */
    public static SelectorProvider provider() {
        synchronized (LATCH) {
            if (found != null) {
                return found;
            }
            SelectorProvider p = fromProperty();
            if (p == null) {
                p = fromServiceLoader();
            }
            if (p == null) {
                // Tier 3: the built-in provider. This used to throw, on the grounds that the VM had
                // no network natives. It has them --`jdk.internal.net.Net`-- and since it also has
                // `poll`, the one call a selector cannot do without, the provider can open
                // selectors and pipes for real. See `KajiSelectorProvider`.
                p = java.nio.channels.AsyncChannelFactory.selectorProvider();
            }
            found = p;
            return p;
        }
    }

    /**
     * The provider the system property names, or null if it is not set.
     *
     * <p>The three failures are told apart because they are fixed differently: **not found** is a
     * class that is not in the classpath, **could not be instantiated** is one that is there but
     * has no **public** no-argument constructor, and **not a subtype** is one that was instantiated
     * but does not serve.
     *
     * <p>`getConstructor()` is used and not `getDeclaredConstructor()` on purpose: the JDK demands
     * that the constructor be public, and accepting a package-private one would make a provider
     * that works here not work there, which is the worst kind of divergence --the one that is
     * discovered in the other VM--.
     *
     * <p>One divergence that **does** remain, measured: when the class exists and is instantiated
     * but is not a `SelectorProvider`, the JDK lets the {@link ClassCastException} of its cast
     * escape and, since the search lives inside a static `Holder` for it, the caller receives it
     * wrapped in `ExceptionInInitializerError` (and the following calls in `NoClassDefFoundError`).
     * That is a consequence of the `Holder`, not of the contract. Here the search lives in the
     * method, so that wrapping can neither be reproduced nor is it desirable:
     * `ServiceConfigurationError` is reported with the name of the class inside, which is the type
     * this same method already uses for the other two configuration failures and says what the
     * problem is instead of hiding it two levels down.
     */
    private static SelectorProvider fromProperty() {
        String name;
        try {
            name = System.getProperty(KEY);
        } catch (SecurityException ignored) {
            // With no permission to read it, it is the same as not being set: it goes on to tier 2.
            return null;
        }
        if (name == null || name.length() == 0) {
            return null;
        }
        Class<?> cls;
        try {
            cls = Class.forName(name, false, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
            throw new ServiceConfigurationError(KEY + ": provider " + name + " not found", e);
        }
        Object instance;
        try {
            instance = cls.getConstructor().newInstance();
        } catch (Exception e) {
            throw new ServiceConfigurationError(
                    KEY + ": provider " + name + " could not be instantiated", e);
        }
        if (!(instance instanceof SelectorProvider)) {
            throw new ServiceConfigurationError(
                    KEY + ": provider " + name + " not a subtype");
        }
        return (SelectorProvider) instance;
    }

    /**
     * The first provider declared in the classpath, or null if there is none.
     *
     * <p>Today it always gives null; the why is in the header and is not a decision of this method.
     */
    private static SelectorProvider fromServiceLoader() {
        try {
            ServiceLoader<SelectorProvider> sl = ServiceLoader.load(SelectorProvider.class);
            Iterator<SelectorProvider> it = sl.iterator();
            if (it.hasNext()) {
                return it.next();
            }
        } catch (Throwable ignored) {
            // A broken provider cannot stop the next tier from being tried.
        }
        return null;
    }

    // ---- the contract of the factory -------------------------------------------------------------

    /** A new datagram channel. */
    public abstract DatagramChannel openDatagramChannel() throws IOException;

    /**
     * A new datagram channel of the family `family`.
     *
     * <p>Abstract, as in the JDK: there is no reasonable default --a provider that knows about
     * families has to say which ones-- and that is why the decision is pushed to whoever implements
     * it.
     *
     * @param family the protocol family, e.g. {@link java.net.StandardProtocolFamily#INET}
     */
    public abstract DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException;

    /** A new pipe, with its two ends. */
    public abstract Pipe openPipe() throws IOException;

    /** A new selector. */
    public abstract AbstractSelector openSelector() throws IOException;

    /** A new listening channel, not yet tied. */
    public abstract ServerSocketChannel openServerSocketChannel() throws IOException;

    /**
     * A new listening channel of the family `family`.
     *
     * <p>Concrete and not abstract on purpose, just as in the JDK: it was added to the API long
     * after the rest, and making it abstract would have broken every provider already written. The
     * default throws, which is the right answer for a provider that does not know about families.
     *
     * @throws UnsupportedOperationException always, unless the provider overrides it
     */
    public ServerSocketChannel openServerSocketChannel(ProtocolFamily family) throws IOException {
        throw new UnsupportedOperationException("Protocol family not supported");
    }

    /** A new socket channel, not yet connected. */
    public abstract SocketChannel openSocketChannel() throws IOException;

    /**
     * A new socket channel of the family `family`.
     *
     * <p>The same note as {@link #openServerSocketChannel(ProtocolFamily)} applies.
     *
     * @throws UnsupportedOperationException always, unless the provider overrides it
     */
    public SocketChannel openSocketChannel(ProtocolFamily family) throws IOException {
        throw new UnsupportedOperationException("Protocol family not supported");
    }

    /**
     * The channel inherited from the process that launched this one, if there is one.
     *
     * <p>It returns `null`, which is the right answer and not a hole: it is what the JDK returns
     * when none was inherited, and this VM never inherits one because it is not started from an
     * `inetd`. A `null` here means exactly what the contract says it means.
     */
    public Channel inheritedChannel() throws IOException {
        return null;
    }
}
