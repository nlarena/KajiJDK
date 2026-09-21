package javax.management.remote.rmi;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

/**
 * The client side: it connects to a JMX server over RMI.
 *
 * <h2>Two ways of saying where to go</h2>
 *
 * <p>With a {@link JMXServiceURL} the remote object has to be <strong>found</strong>: either it
 * comes encoded inside the address itself, or it has to be looked up in a JNDI directory. With
 * an {@link RMIServer} it is already at hand, and connecting is asking it for a connection.
 *
 * <h2>Connecting is not opening a socket</h2>
 *
 * <p>It is calling {@link RMIServer#newClient}, which authenticates and returns this client's
 * own connection. Only after that does {@link #getMBeanServerConnection} have something to
 * return: an {@link MBeanServerConnection} that looks local and underneath packs every call.
 *
 * <p>Everything done before {@link #connect} fails with {@code IOException: Not connected}, and
 * everything done after {@link #close} with {@code IOException: Connector closed}. The two
 * messages are different on purpose: "not yet" and "no longer" are two different errors of
 * the caller.
 *
 * <h2>The connection notifications</h2>
 *
 * <p>This object is itself a notification broadcaster, and it emits three: opened, closed and
 * failed. It is how a client learns that it was left without a server without having to discover
 * it in the middle of a call.
 *
 * <h2>State in this VM</h2>
 *
 * <p>With an {@link RMIServer} of this same process it <strong>works entirely</strong>: it
 * connects, returns an {@link MBeanServerConnection} that reaches the
 * {@link javax.management.MBeanServer}, delivers notifications and closes. It is the path
 * {@code java/RMI1.java} exercises.
 *
 * <p>With a {@link JMXServiceURL} it cannot: finding the remote object is the transport, and
 * this VM does not have it. In that case {@link #connect} throws {@link IOException} saying so,
 * instead of returning a connector that would then fail on the first call.
 *
 * @since 1.5
 */
public class RMIConnector implements JMXConnector, Serializable {

    private static final long serialVersionUID = 817323035842634473L;

    /** So that two notifications of the same connection arrive in order. */
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private final RMIServer rmiServer;
    private final JMXServiceURL jmxServiceURL;

    private transient NotificationBroadcasterSupport broadcaster =
        new NotificationBroadcasterSupport();
    private transient RMIConnection connection;
    private transient RemoteConnection serverConnection;
    private transient String connectionId;
    private transient boolean closed;

    /**
     * A connector to that address.
     *
     * @param url the server's address
     * @param environment the configuration properties, or {@code null}
     * @throws IllegalArgumentException if {@code url} is {@code null}
     */
    public RMIConnector(JMXServiceURL url, Map<String, ?> environment) {
        this(null, url, environment);
    }

    /**
     * A connector to a server one already has.
     *
     * @param rmiServer the server
     * @param environment the configuration properties, or {@code null}
     * @throws IllegalArgumentException if {@code rmiServer} is {@code null}
     */
    public RMIConnector(RMIServer rmiServer, Map<String, ?> environment) {
        this(rmiServer, null, environment);
    }

    private RMIConnector(RMIServer rmiServer, JMXServiceURL url, Map<String, ?> environment) {
        if (rmiServer == null && url == null) {
            throw new IllegalArgumentException("rmiServer and jmxServiceURL both null");
        }
        this.rmiServer = rmiServer;
        this.jmxServiceURL = url;
    }

    /**
     * The server's address, if it was built with one.
     *
     * @return the address, or {@code null} if it was built with an {@link RMIServer}
     */
    public JMXServiceURL getAddress() {
        return jmxServiceURL;
    }

    /**
     * Connects without additional properties.
     *
     * @throws IOException if it could not connect
     */
    public void connect() throws IOException {
        connect(null);
    }

    /**
     * Connects.
     *
     * <p>On an already connected connector it does nothing: it is what allows calling it from
     * several places without coordinating who connects first.
     *
     * @param environment the configuration properties, or {@code null}; the credential goes in
     *     {@code jmx.remote.credentials}
     * @throws IOException if it could not connect, or if the connector is already closed
     */
    public synchronized void connect(Map<String, ?> environment) throws IOException {
        if (closed) {
            throw new IOException("Connector closed");
        }
        if (connection != null) {
            return;
        }
        if (rmiServer == null) {
            throw new IOException("this VM has no RMI transport: there is no way to "
                    + "find the remote object of " + jmxServiceURL);
        }
        final Map<String, ?> env = environment == null
                ? Collections.<String, Object>emptyMap() : new HashMap<String, Object>(environment);
        connection = rmiServer.newClient(env.get(CREDENTIALS));
        connectionId = connection.getConnectionId();
        serverConnection = new RemoteConnection(connection);
        broadcaster.sendNotification(new JMXConnectionNotification(
                JMXConnectionNotification.OPENED, this, connectionId,
                SEQUENCE.getAndIncrement(), "Connection opened", null));
    }

    /**
     * The identifier the server gave this connection.
     *
     * @return the identifier
     * @throws IOException if it has not connected yet or is already closed
     */
    public synchronized String getConnectionId() throws IOException {
        requireConnected();
        return connectionId;
    }

    /**
     * The connection to the server's {@link javax.management.MBeanServer}.
     *
     * @return the connection
     * @throws IOException if it has not connected yet or is already closed
     */
    public synchronized MBeanServerConnection getMBeanServerConnection() throws IOException {
        requireConnected();
        return serverConnection;
    }

    private void requireConnected() throws IOException {
        if (closed) {
            throw new IOException("Connector closed");
        }
        if (connection == null) {
            throw new IOException("Not connected");
        }
    }

    /**
     * Registers a listener of the connection's notifications.
     *
     * @param listener the listener
     * @param filter the filter, or {@code null}
     * @param handback what is handed back to it with each notification, or {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addConnectionNotificationListener(NotificationListener listener,
            NotificationFilter filter, Object handback) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        broadcaster.addNotificationListener(listener, filter, handback);
    }

    /**
     * Removes a listener of the connection's notifications.
     *
     * @param listener the listener
     * @throws ListenerNotFoundException if it was not registered
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void removeConnectionNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        broadcaster.removeNotificationListener(listener);
    }

    /**
     * Removes the listener registered with that filter and that object.
     *
     * @param l the listener
     * @param f the filter it was registered with
     * @param handback the object it was registered with
     * @throws ListenerNotFoundException if it was not registered that way
     * @throws NullPointerException if {@code l} is {@code null}
     */
    public void removeConnectionNotificationListener(NotificationListener l, NotificationFilter f,
            Object handback) throws ListenerNotFoundException {
        if (l == null) {
            throw new NullPointerException("listener");
        }
        broadcaster.removeNotificationListener(l, f, handback);
    }

    /**
     * Closes the connection.
     *
     * <p>On one that never connected it does nothing beyond leaving it closed. It is on purpose:
     * closing something that was not opened is not an error, and forcing everybody to check first
     * would only add noise to everybody's {@code finally} block.
     *
     * @throws IOException if the server could not close the connection
     */
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (serverConnection != null) {
            serverConnection.close();
        }
        final String id = connectionId;
        try {
            if (connection != null) {
                connection.close();
            }
        } finally {
            connection = null;
            serverConnection = null;
            if (id != null) {
                broadcaster.sendNotification(new JMXConnectionNotification(
                        JMXConnectionNotification.CLOSED, this, id,
                        SEQUENCE.getAndIncrement(), "Connection closed", null));
            }
        }
    }

    /**
     * A description, with the address or with the server.
     *
     * @return the description
     */
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder(getClass().getName()).append(": ");
        if (rmiServer != null) {
            b.append("rmiServer=").append(rmiServer);
        } else {
            b.append("jmxServiceURL=").append(jmxServiceURL);
        }
        return b.toString();
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // The transient fields are not serialized and would be left null: a deserialized connector
        // has to be able to register listeners before connecting, just like a freshly built one.
        broadcaster = new NotificationBroadcasterSupport();
    }
}
