package javax.management.remote.rmi;

import java.io.Closeable;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnectorServer;
import javax.security.auth.Subject;

/**
 * The part of the server that does not depend on the transport.
 *
 * <h2>What stays on this side of the line</h2>
 *
 * <p>It keeps the list of connected clients, the {@link MBeanServer} that is worked against, the
 * default class loader and the life cycle. None of that changes according to how one gets here,
 * and that is why it is written only once.
 *
 * <p>What it leaves abstract is exactly what does change: {@link #export} and {@link #toStub}
 * publish the object over the concrete transport, and {@link #makeClient} builds the connection.
 * It is the separation between "what a JMX server does" and "how it is reached".
 *
 * <h2>{@link #newClient} is the door</h2>
 *
 * <p>It authenticates --if there is a {@link JMXAuthenticator} in the environment--, builds the
 * connection's identifier, creates it with {@link #makeClient} and notes it down. Each client
 * gets its own, which is what allows shutting the door on one without touching the others.
 *
 * <p>{@link #close} closes them all: first the transport so that no more come in, then each live
 * connection. The order matters, because the other way round a new client would come in while
 * the old ones are being closed.
 *
 * <h2>State in this library</h2>
 *
 * <p>Everything in this class really works: the authentication, the client list, the
 * identifiers, the cascading close. What cannot work is the abstract part, and that is decided
 * by the subclass: {@link RMIJRMPServerImpl} needs to export over RMI, and this VM does not have
 * that transport.
 *
 * @since 1.5
 */
public abstract class RMIServerImpl implements Closeable, RMIServer {

    private final Map<String, ?> env;
    private final List<RMIConnection> clients = new ArrayList<RMIConnection>();

    private ClassLoader cl;
    private MBeanServer mbeanServer;
    private boolean closed;
    private int connectionCount;

    /**
     * A server with that environment.
     *
     * @param env the configuration properties, or {@code null}
     */
    public RMIServerImpl(Map<String, ?> env) {
        this.env = env == null ? Collections.<String, Object>emptyMap() : env;
    }

    /**
     * Publishes this object over the concrete transport.
     *
     * @throws IOException if it could not be published
     */
    protected abstract void export() throws IOException;

    /**
     * The remote object that has to be sent to the client so that it reaches here.
     *
     * @return the stub
     * @throws IOException if it could not be obtained
     */
    public abstract Remote toStub() throws IOException;

    /**
     * Sets the loader with which what the clients send is deserialized.
     *
     * <p>It is a security decision and not a convenience one: it defines which classes a client can
     * make appear inside this process.
     *
     * @param cl the loader
     */
    public synchronized void setDefaultClassLoader(ClassLoader cl) {
        this.cl = cl;
    }

    /**
     * The default loader.
     *
     * @return the loader, or {@code null}
     */
    public synchronized ClassLoader getDefaultClassLoader() {
        return cl;
    }

    /**
     * Sets the {@link MBeanServer} the connections work against.
     *
     * @param mbs the MBean server
     */
    public synchronized void setMBeanServer(MBeanServer mbs) {
        this.mbeanServer = mbs;
    }

    /**
     * The configured {@link MBeanServer}.
     *
     * @return the MBean server, or {@code null}
     */
    public synchronized MBeanServer getMBeanServer() {
        return mbeanServer;
    }

    /**
     * The protocol's and the provider's version.
     *
     * <p>The format is the protocol's version, a space, and the implementation's name. The second
     * half comes from {@code java.runtime.version}: it is on purpose that it says which runtime is
     * running, because it is what the client looks at when the two ends do not understand each
     * other.
     *
     * @return the version
     */
    public String getVersion() {
        try {
            return "1.0 java_runtime_" + System.getProperty("java.runtime.version");
        } catch (SecurityException e) {
            return "1.0 ";
        }
    }

    /**
     * Authenticates the client and opens its connection.
     *
     * @param credentials the credential, or {@code null}
     * @return the client's connection
     * @throws IOException if the server is closed or the connection could not be created
     * @throws IllegalStateException if no {@link MBeanServer} has been set on it yet
     * @throws SecurityException if the credential does not serve
     */
    public RMIConnection newClient(Object credentials) throws IOException {
        // The order is the JDK's and it does matter: first it is checked that there is an
        // MBeanServer and only then is the client authenticated. The other way round, a badly
        // assembled server would ask the client for the credential only to tell it afterwards that
        // it was not ready -- that is, it would make it send a secret to something that cannot
        // serve it.
        synchronized (this) {
            if (closed) {
                throw new IOException("The server has been closed");
            }
            if (mbeanServer == null) {
                throw new IllegalStateException("Not attached to an MBean server");
            }
        }
        final Subject subject = authenticate(credentials);
        final String id;
        synchronized (this) {
            id = newConnectionId(getProtocol(), subject);
        }
        // makeClient is left outside the synchronized block on purpose: it belongs to the subclass,
        // it may take a while --exporting opens a port-- and having it inside would leave the whole
        // server blocked while a single client connects.
        final RMIConnection c = makeClient(id, subject);
        synchronized (this) {
            clients.add(c);
        }
        return c;
    }

    /**
     * Authenticates with the environment's {@link JMXAuthenticator}, if there is one.
     *
     * <p>Without an authenticator the connection is accepted with no subject: it is the default
     * configuration, and it is the reason a JMX server is not published on a network that is not
     * trusted.
     *
     * <p>The JDK looks one step further: if there is no authenticator but
     * {@code jmx.remote.x.password.file} or {@code jmx.remote.x.login.config} is set, it builds a
     * {@code JMXPluggableAuthenticator} from the file. This one does not: only
     * {@code jmx.remote.authenticator} is looked at, so those two properties authenticate nobody
     * here.
     */
    private Subject authenticate(Object credentials) {
        final Object a = env.get(JMXConnectorServer.AUTHENTICATOR);
        if (a == null) {
            return null;
        }
        return ((JMXAuthenticator) a).authenticate(credentials);
    }

    /**
     * The identifier the connection being opened gets.
     *
     * <p>It carries the protocol, the client's machine, who authenticated and a number that does
     * not repeat. It serves so that the server's log says something useful: without the protocol
     * and without the number, two connections of the same user would be indistinguishable in the
     * log file.
     *
     * <p>The client's machine is known only <strong>during</strong> a remote call, so when there is
     * none in progress it is left out. Here there never is one, because there is no transport.
     */
    private String newConnectionId(String protocol, Subject subject) {
        connectionCount++;
        String host = "";
        try {
            host = RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            host = "";
        }
        final StringBuilder b = new StringBuilder();
        b.append(protocol).append(':');
        if (host.length() > 0) {
            b.append("//").append(host);
        }
        b.append(' ');
        if (subject != null) {
            final Set<java.security.Principal> ps = subject.getPrincipals();
            String sep = "";
            for (final java.security.Principal p : ps) {
                final String name = p.getName().replace(' ', '_').replace(';', ':');
                b.append(sep).append(name);
                sep = ";";
            }
        }
        b.append(' ').append(connectionCount);
        return b.toString();
    }

    /**
     * Builds a client's connection.
     *
     * @param connectionId the identifier it gets
     * @param subject who authenticated, or {@code null}
     * @return the connection
     * @throws IOException if it could not be created
     */
    protected abstract RMIConnection makeClient(String connectionId, Subject subject)
            throws IOException;

    /**
     * Closes a client's connection.
     *
     * @param client the connection
     * @throws IOException if it could not be closed
     */
    protected abstract void closeClient(RMIConnection client) throws IOException;

    /**
     * The name of this transport's protocol, such as {@code "rmi"}.
     *
     * @return the protocol
     */
    protected abstract String getProtocol();

    /**
     * Notice that a connection closed by itself.
     *
     * <p>The connection itself calls it. Removing it from the list here --and not only in
     * {@link #close}-- is what keeps a long-lived server from piling up dead connections.
     *
     * @param client the connection that closed
     * @throws IOException if it could not be processed
     * @throws NullPointerException if {@code client} is {@code null}
     */
    protected void clientClosed(RMIConnection client) throws IOException {
        if (client == null) {
            throw new NullPointerException("Null client");
        }
        synchronized (this) {
            clients.remove(client);
        }
        closeClient(client);
    }

    /**
     * Closes the server and all the live connections.
     *
     * <p>First it stops accepting and then it closes the ones there are: the other way round a new
     * client would come in while the old ones are being closed.
     *
     * @throws IOException if something could not be closed
     */
    public void close() throws IOException {
        final List<RMIConnection> copy;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            copy = new ArrayList<RMIConnection>(clients);
            clients.clear();
        }
        IOException first = null;
        try {
            closeServer();
        } catch (IOException e) {
            first = e;
        }
        for (final RMIConnection c : copy) {
            try {
                closeClient(c);
            } catch (IOException e) {
                // A connection that does not close cannot keep the others from closing: the first
                // failure is kept, all of them go on being closed, and only at the end is it
                // thrown.
                if (first == null) {
                    first = e;
                }
            }
        }
        if (first != null) {
            throw first;
        }
    }

    /**
     * Closes the transport.
     *
     * @throws IOException if it could not be closed
     */
    protected abstract void closeServer() throws IOException;

    /** The environment it was built with; never {@code null}. For the package's subclasses. */
    Map<String, ?> environment() {
        return env;
    }
}
