package javax.management.remote.rmi;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerForwarder;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;

/**
 * The JMX server that is published over RMI.
 *
 * <h2>What it does</h2>
 *
 * <p>It is the object that is registered as an MBean and that is started and stopped. Inside it
 * has an {@link RMIServerImpl} --the one that really serves-- and an address; its job is the
 * life cycle and the publication, not the calls.
 *
 * <h2>Starting is publishing</h2>
 *
 * <p>{@link #start} exports the {@link RMIServerImpl} and, if the address asks for it, notes it
 * down in a JNDI directory so that the client can find it by name. Only then does
 * {@link #getAddress} return a complete address, with the stub inside or with the name in the
 * directory.
 *
 * <p>A stopped server cannot be started again. It is on purpose: the state released on stopping
 * --the port, the registration-- is not rebuilt, and letting it "resume" halfway would be
 * worse than forcing another one to be created.
 *
 * <h2>{@link #getAttributes} does not return the environment</h2>
 *
 * <p>It returns the environment <strong>minus what cannot be shown</strong>: the authenticator,
 * the socket factories, the key files, the JNDI credentials. The list is in {@code HIDDEN} and
 * can be changed with {@code jmx.remote.x.hidden.attributes}.
 *
 * <p>Neither does it return what is not serializable. That rule is not one of security but a
 * practical one: these attributes are read through the JMX connection itself, so they have to be
 * able to travel.
 *
 * <h2>State in this VM</h2>
 *
 * <p>Everything works except {@link #start}, which has to export over RMI. The life cycle, the
 * attributes, the address, the MBeanServer forwarder and the connection notifications are real
 * and behave as in the JDK: a freshly created server is not active, {@link #getAddress} returns
 * the address it was built with, and {@link #stop} on one that never started does nothing.
 *
 * @since 1.5
 */
public class RMIConnectorServer extends JMXConnectorServer {

    /**
     * Whether noting itself down in the JNDI directory may overwrite an earlier entry.
     *
     * <p>The value is the text {@code "true"} or {@code "false"}. Overwriting is convenient while
     * developing and dangerous in production: two servers with the same address and the second wins
     * in silence.
     */
    public static final String JNDI_REBIND_ATTRIBUTE = "jmx.remote.jndi.rebind";

    /** The client's socket factory; it travels inside the stub. */
    public static final String RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE =
            "jmx.remote.rmi.client.socket.factory";

    /** The server's socket factory; it stays on this side. */
    public static final String RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE =
            "jmx.remote.rmi.server.socket.factory";

    /**
     * The deserialization filter applied to the credential the client sends.
     *
     * <p>It is the first thing that arrives from outside and nobody has authenticated yet: without
     * a filter, any client makes the server build whatever object graph it likes
     * <strong>before</strong> it is checked whether it has permission.
     *
     * @since 10
     */
    public static final String CREDENTIALS_FILTER_PATTERN =
            "jmx.remote.rmi.server.credentials.filter.pattern";

    /**
     * The deserialization filter for everything else that arrives over the connection.
     *
     * @since 10
     */
    public static final String SERIAL_FILTER_PATTERN = "jmx.remote.rmi.server.serial.filter.pattern";

    /** The property with which the list of attributes not shown is changed. */
    static final String HIDDEN_ATTRIBUTES = "jmx.remote.x.hidden.attributes";

    /**
     * The attributes {@link #getAttributes} does not show.
     *
     * <p>An entry ending in a dot is a prefix and covers everything that starts that way; the rest
     * are exact names. It is the JDK's list --{@code EnvHelp.DEFAULT_HIDDEN_ATTRIBUTES}-- and not a
     * choice: changing it would make a server of this library expose over the network something the
     * same server in the JDK does not expose.
     *
     * <p>The JDK writes the prefix with a trailing {@code *} instead of a dot, which matters for a
     * list written by hand: see {@link #hiddenFrom}.
     */
    static final String[] HIDDEN = {
        "java.naming.security.",
        "jmx.remote.authenticator",
        "jmx.remote.context",
        "jmx.remote.default.class.loader",
        "jmx.remote.message.connection.server",
        "jmx.remote.object.wrapping",
        "jmx.remote.rmi.client.socket.factory",
        "jmx.remote.rmi.server.socket.factory",
        "jmx.remote.sasl.callback.handler",
        "jmx.remote.tls.socket.factory",
        "jmx.remote.x.access.file",
        "jmx.remote.x.password.file",
    };

    private final Map<String, ?> env;
    private final Map<String, Object> attributes;

    private final JMXServiceURL requestedUrl;

    private RMIServerImpl rmiServerImpl;
    private JMXServiceURL address;
    private boolean started;
    private boolean stopped;

    /**
     * A server at that address.
     *
     * @param url the address, or {@code null} for one on a port the system picks
     * @param environment the configuration properties, or {@code null}
     * @throws IOException if it could not be created
     */
    public RMIConnectorServer(JMXServiceURL url, Map<String, ?> environment) throws IOException {
        this(url, environment, (RMIServerImpl) null, null);
    }

    /**
     * A server at that address, over that {@link MBeanServer}.
     *
     * @param url the address, or {@code null}
     * @param environment the configuration properties, or {@code null}
     * @param mbeanServer the MBean server, or {@code null} for the one that registers it
     * @throws IOException if it could not be created
     */
    public RMIConnectorServer(JMXServiceURL url, Map<String, ?> environment,
            MBeanServer mbeanServer) throws IOException {
        this(url, environment, (RMIServerImpl) null, mbeanServer);
    }

    /**
     * A server at that address.
     *
     * @param url the address, or {@code null} for one on a port the system picks
     * @param environment the configuration properties, or {@code null}
     * @throws IOException if it could not be created
     */
    public RMIConnectorServer(JMXServiceURL url, Map<String, ?> environment,
            RMIServerImpl rmiServerImpl, MBeanServer mbeanServer) throws IOException {
        super(mbeanServer);
        if (url != null) {
            final String p = url.getProtocol();
            if (!"rmi".equalsIgnoreCase(p) && !"iiop".equalsIgnoreCase(p)) {
                throw new MalformedURLException("Invalid protocol type: " + p);
            }
        }
        this.requestedUrl = url;
        this.rmiServerImpl = rmiServerImpl;
        this.env = environment == null
                ? Collections.<String, Object>emptyMap() : new HashMap<String, Object>(environment);
        this.attributes = Collections.unmodifiableMap(filterHidden(this.env));
    }

    /**
     * The environment, without what cannot be shown and without what cannot travel.
     *
     * @return the attributes, in a map that cannot be modified
     */
    public Map<String, ?> getAttributes() {
        return attributes;
    }

    /**
     * The address at which it can be reached.
     *
     * <p>It is {@code null} until it is started, even if it was built with an address. It is not an
     * oversight: the address it is built with is a request --which port, which name-- and the real
     * one is known only once it has been exported, because it may have the port the system assigned
     * and the stub encoded inside. Returning the requested one would be vouching for an address
     * where nobody is serving yet.
     *
     * @return the address, or {@code null} if it has not started yet
     */
    public JMXServiceURL getAddress() {
        return address;
    }

    /**
     * Whether it is started and has not been stopped yet.
     *
     * @return true if it is serving
     */
    public synchronized boolean isActive() {
        return started && !stopped;
    }

    /**
     * Starts the server: it exports the {@link RMIServerImpl} and publishes it.
     *
     * @throws IOException if it could not be started, or if it had already been stopped
     * @throws UnsupportedOperationException in this VM, which has no RMI transport
     */
    public synchronized void start() throws IOException {
        if (stopped) {
            throw new IOException("The server has been stopped.");
        }
        if (started) {
            return;
        }
        if (getMBeanServer() == null) {
            throw new IllegalStateException("This connector server is not attached to an "
                    + "MBean server");
        }
        if (rmiServerImpl == null) {
            rmiServerImpl = new RMIJRMPServerImpl(0, null, null, env);
        }
        rmiServerImpl.setMBeanServer(getMBeanServer());
        rmiServerImpl.export();
        address = requestedUrl;
        started = true;
    }

    /**
     * Stops the server and closes whatever connections there are.
     *
     * <p>On one that never started it does nothing beyond marking it as stopped, so that it cannot
     * be started afterwards.
     *
     * @throws IOException if something could not be closed
     */
    public void stop() throws IOException {
        final RMIServerImpl s;
        synchronized (this) {
            if (stopped) {
                return;
            }
            stopped = true;
            s = rmiServerImpl;
        }
        if (s != null) {
            s.close();
        }
    }

    /**
     * A client already connected to this server.
     *
     * @param environment the client's configuration properties, or {@code null}
     * @return the connector
     * @throws IOException if it could not be created
     * @throws IllegalStateException if the server is not active
     */
    public JMXConnector toJMXConnector(Map<String, ?> environment) throws IOException {
        synchronized (this) {
            if (!isActive()) {
                throw new IllegalStateException("Connector is not active");
            }
        }
        return super.toJMXConnector(environment);
    }

    /**
     * Puts a forwarder in front of the {@link MBeanServer}.
     *
     * <p>It is how authorization filters are interposed: the forwarder sees every call before the
     * MBean server does. If the server has already started, the {@link RMIServerImpl} has to be
     * told as well, because the live connections ask it for the MBeanServer.
     *
     * @param mbsf the forwarder
     */
    public synchronized void setMBeanServerForwarder(MBeanServerForwarder mbsf) {
        super.setMBeanServerForwarder(mbsf);
        if (rmiServerImpl != null) {
            rmiServerImpl.setMBeanServer(getMBeanServer());
        }
    }

    /**
     * Reports that a connection was opened.
     *
     * @param connectionId the connection's identifier
     * @param message the message
     * @param userData whatever is to be attached, or {@code null}
     */
    @Override
    protected void connectionOpened(String connectionId, String message, Object userData) {
        super.connectionOpened(connectionId, message, userData);
    }

    /**
     * Reports that a connection was closed.
     *
     * @param connectionId the connection's identifier
     * @param message the message
     * @param userData whatever is to be attached, or {@code null}
     */
    @Override
    protected void connectionClosed(String connectionId, String message, Object userData) {
        super.connectionClosed(connectionId, message, userData);
    }

    /**
     * Reports that a connection failed.
     *
     * @param connectionId the connection's identifier
     * @param message the message
     * @param userData whatever is to be attached, or {@code null}
     */
    @Override
    protected void connectionFailed(String connectionId, String message, Object userData) {
        super.connectionFailed(connectionId, message, userData);
    }

    /** The environment without what is hidden and without what is not serializable. */
    private static Map<String, Object> filterHidden(Map<String, ?> env) {
        final String[] hidden = hiddenFrom(env);
        final Map<String, Object> out = new HashMap<String, Object>();
        for (final Map.Entry<String, ?> e : env.entrySet()) {
            final String k = e.getKey();
            if (k == null || isHidden(k, hidden)) {
                continue;
            }
            if (e.getValue() != null && !(e.getValue() instanceof Serializable)) {
                continue;
            }
            out.put(k, e.getValue());
        }
        return out;
    }

    private static String[] hiddenFrom(Map<String, ?> env) {
        final Object v = env.get(HIDDEN_ATTRIBUTES);
        if (!(v instanceof String)) {
            return HIDDEN;
        }
        // A list of one's own is written separated by spaces and <b>replaces</b> the usual one
        // instead of extending it. The JDK does the opposite: it adds the given list to the default
        // one unless the value starts with `=`, and there a prefix is written with a trailing `*`
        // rather than a dot. A list written for the JDK therefore does not mean the same thing
        // here.
        final String s = ((String) v).trim();
        return s.isEmpty() ? new String[0] : s.split("\\s+");
    }

    private static boolean isHidden(String key, String[] hidden) {
        for (final String o : hidden) {
            if (o.endsWith(".") ? key.startsWith(o) : key.equals(o)) {
                return true;
            }
        }
        return false;
    }
}
