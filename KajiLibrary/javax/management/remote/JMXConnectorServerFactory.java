package javax.management.remote;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import javax.management.MBeanServer;

/**
 * KajiLibrary's javax.management.remote.JMXConnectorServerFactory -- gets a connector server.
 *
 * <p>{@link JMXConnectorFactory}'s mirror for the server side, with a single method. The server
 * it returns is <b>not started</b>: {@code start()} has to be called on it, and that is on
 * purpose, so that it can be registered as an MBean or have interceptors chained to it before
 * opening the port.
 *
 * <h2>How a protocol's provider is found</h2>
 *
 * <p>Two paths are tried, in order:
 *
 * <ol>
 *   <li>those declared as a service and found with {@link java.util.ServiceLoader}. It is the
 *       modern way and the one that asks for no configuration;
 *   <li>by <b>deduced class name</b>: for each package of the
 *       {@link #PROTOCOL_PROVIDER_PACKAGES} property, the class
 *       {@code <package>.<protocol>.ServerProvider} is looked for. The packages are separated with
 *       {@code |}, and the protocol is translated by turning {@code +} into a dot and
 *       {@code -} into an underscore, because a protocol may have characters a package name
 *       does not admit.
 * </ol>
 *
 * <p>If a provider recognizes the protocol but cannot cope with that environment, it throws
 * {@link JMXProviderException} and the next one is tried. If none recognizes it,
 * {@link java.net.MalformedURLException} comes out with {@code "Unsupported protocol"}.
 *
 * <p>That distinction is what is of use to the caller: the first says "it is broken", the
 * second says "it does not exist". See {@link JMXProviderException}.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This library ships no protocol. RMI needs a remote transport layer that is not there, and
 * JMXMP was never in the JDK. The search is really implemented --it walks the
 * {@link java.util.ServiceLoader} and tries the deduced names-- and ends in
 * {@code "Unsupported protocol"}, which is exactly what JDK 25 does with a protocol nobody
 * provides. Adding a provider, this works with no changes.
 */
public class JMXConnectorServerFactory {

    /** Environment key: with what class loader to deserialize what arrives. */
    public static final String DEFAULT_CLASS_LOADER = "jmx.remote.default.class.loader";

    /**
     * Environment key: the {@code ObjectName} of the class loader MBean to use.
     *
     * <p>It is the alternative to {@link #DEFAULT_CLASS_LOADER} and they are exclusive: one gives
     * the loader, the other names it so that it is looked up in the MBean server. Giving both is an
     * error.
     */
    public static final String DEFAULT_CLASS_LOADER_NAME = "jmx.remote.default.class.loader.name";

    /**
     * Property and environment key: in what packages to look for providers, separated by {@code |}.
     */
    public static final String PROTOCOL_PROVIDER_PACKAGES = "jmx.remote.protocol.provider.pkgs";

    /** Environment key: with what loader to look for the provider's class. */
    public static final String PROTOCOL_PROVIDER_CLASS_LOADER =
        "jmx.remote.protocol.provider.class.loader";

    /** It has no state; the public constructor is the one the JDK left. */
    public JMXConnectorServerFactory() {
    }

    /**
     * An unstarted server for that address.
     *
     * @param mbeanServer which MBean server it exposes, or null to tie it when registering it
     * @throws MalformedURLException if there is no provider for that protocol
     * @throws JMXProviderException if there is one and it could not
     * @throws IOException if it failed for something else
     */
    public static JMXConnectorServer newJMXConnectorServer(JMXServiceURL serviceURL,
                                                           Map<String, ?> environment,
                                                           MBeanServer mbeanServer)
        throws IOException {
        if (serviceURL == null) {
            throw new NullPointerException("Null JMXServiceURL");
        }
        Map<String, Object> env;
        if (environment == null) {
            env = new HashMap<String, Object>();
        } else {
            FactorySupport.checkKeys(environment);
            env = new HashMap<String, Object>(environment);
        }
        String protocol = serviceURL.getProtocol();
        Iterator<JMXConnectorServerProvider> loaded =
            ServiceLoader.load(JMXConnectorServerProvider.class).iterator();
        while (hasNextQuietly(loaded)) {
            JMXConnectorServerProvider p = loaded.next();
            JMXConnectorServer made = p.newJMXConnectorServer(serviceURL, env, mbeanServer);
            if (made != null) {
                return made;
            }
        }
        JMXConnectorServerProvider named =
            FactorySupport.byName(env, protocol, "ServerProvider",
                                  JMXConnectorServerProvider.class);
        if (named != null) {
            JMXConnectorServer made = named.newJMXConnectorServer(serviceURL, env, mbeanServer);
            if (made != null) {
                return made;
            }
        }
        throw new MalformedURLException("Unsupported protocol: " + protocol);
    }

    /** See {@link JMXConnectorFactory}: a broken provider does not bring the search down. */
    private static boolean hasNextQuietly(Iterator<JMXConnectorServerProvider> it) {
        try {
            return it.hasNext();
        } catch (Throwable e) {
            return false;
        }
    }
}
