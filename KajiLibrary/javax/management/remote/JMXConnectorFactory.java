package javax.management.remote;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * KajiLibrary's javax.management.remote.JMXConnectorFactory -- gets a client connector.
 *
 * <p>The client side's entry point. {@link #connect} is the normal shortcut;
 * {@link #newJMXConnector} returns the connector <b>unconnected</b>, which is what is needed to
 * register listeners before anything happens -- see {@link JMXConnector}.
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
 *       {@code <package>.<protocol>.ClientProvider} is looked for. The packages are separated with
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
public class JMXConnectorFactory {

    /** Environment key: with what class loader to deserialize what arrives. */
    public static final String DEFAULT_CLASS_LOADER = "jmx.remote.default.class.loader";

    /**
     * Property and environment key: in what packages to look for providers, separated by {@code |}.
     */
    public static final String PROTOCOL_PROVIDER_PACKAGES = "jmx.remote.protocol.provider.pkgs";

    /** Environment key: with what loader to look for the provider's class. */
    public static final String PROTOCOL_PROVIDER_CLASS_LOADER =
        "jmx.remote.protocol.provider.class.loader";

    /** The packages that are tried if nothing else is said. */
    private static final String DEFAULT_PACKAGES = "com.sun.jmx.remote.protocol";

    /** It has no state; the public constructor is the one the JDK left. */
    public JMXConnectorFactory() {
    }

    /**
     * Creates a connector and connects it.
     *
     * @throws MalformedURLException if there is no provider for that protocol
     * @throws IOException if it could not connect
     * @throws NullPointerException if the address is null
     */
    public static JMXConnector connect(JMXServiceURL serviceURL) throws IOException {
        return connect(serviceURL, null);
    }

    /**
     * The same, with an environment.
     *
     * @throws MalformedURLException if there is no provider for that protocol
     * @throws IOException if it could not connect
     */
    public static JMXConnector connect(JMXServiceURL serviceURL, Map<String, ?> environment)
        throws IOException {
        if (serviceURL == null) {
            throw new NullPointerException("Null JMXServiceURL");
        }
        JMXConnector conn = newJMXConnector(serviceURL, environment);
        conn.connect(environment);
        return conn;
    }

    /**
     * Creates a connector without connecting it. See the class note.
     *
     * @throws MalformedURLException if there is no provider for that protocol
     * @throws JMXProviderException if there is one and it could not
     * @throws IOException if it failed for something else
     */
    public static JMXConnector newJMXConnector(JMXServiceURL serviceURL,
                                               Map<String, ?> environment) throws IOException {
        if (serviceURL == null) {
            throw new NullPointerException("Null JMXServiceURL");
        }
        Map<String, Object> env = copyEnvironment(environment);
        String protocol = serviceURL.getProtocol();
        Iterator<JMXConnectorProvider> loaded =
            ServiceLoader.load(JMXConnectorProvider.class).iterator();
        while (hasNextQuietly(loaded)) {
            JMXConnectorProvider p = loaded.next();
            JMXConnector made = p.newJMXConnector(serviceURL, env);
            if (made != null) {
                return made;
            }
        }
        JMXConnectorProvider named = FactorySupport.byName(env, protocol, "ClientProvider",
                                                           JMXConnectorProvider.class);
        if (named != null) {
            JMXConnector made = named.newJMXConnector(serviceURL, env);
            if (made != null) {
                return made;
            }
        }
        throw new MalformedURLException("Unsupported protocol: " + protocol);
    }

    /** A mutable copy, checking that the keys are strings. */
    private static Map<String, Object> copyEnvironment(Map<String, ?> env) {
        if (env == null) {
            return new HashMap<String, Object>();
        }
        FactorySupport.checkKeys(env);
        return new HashMap<String, Object>(env);
    }

    /** A broken provider cannot bring the search down; the ones that follow may still serve. */
    private static boolean hasNextQuietly(Iterator<JMXConnectorProvider> it) {
        try {
            return it.hasNext();
        } catch (Throwable e) {
            return false;
        }
    }
}
