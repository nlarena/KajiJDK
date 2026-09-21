package javax.management.remote;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.security.auth.Subject;

/**
 * KajiLibrary's javax.management.remote.JMXConnector -- the client side of a JMX connection.
 *
 * <p>It is obtained with {@link JMXConnectorFactory} and used like this: {@link #connect}, then
 * {@link #getMBeanServerConnection} to operate, and {@link #close} at the end. It is
 * {@link Closeable}, so it serves in a try-with-resources.
 *
 * <h2>Creating and connecting are two steps</h2>
 *
 * <p>{@code JMXConnectorFactory.newJMXConnector} returns an <b>unconnected</b> connector. That
 * allows registering the connection listeners before anything happens, which is the only way not
 * to miss the {@code OPENED}. {@code JMXConnectorFactory.connect} does both steps at once and is
 * what is used when that does not matter.
 *
 * <h2>{@link #getMBeanServerConnection(Subject)} is marked</h2>
 *
 * <p>The version with a {@code Subject} serves for acting on another's behalf; its default throws
 * {@link UnsupportedOperationException}. It depends on the delegation mechanism, which became
 * obsolete together with {@link SubjectDelegationPermission}.
 *
 * <h2>The connection identifier</h2>
 *
 * <p>{@link #getConnectionId} is unique and <b>changes if the connection is reopened</b>.
 * Comparing the one seen now against the one seen before is how it is detected that there was a
 * reconnection in between and that the server's state may have changed.
 */
public interface JMXConnector extends Closeable {

    /** The environment key where the credentials go. */
    String CREDENTIALS = "jmx.remote.credentials";

    /**
     * Connects with the environment given when it was created.
     *
     * @throws IOException if it could not
     * @throws SecurityException if it was not allowed
     */
    void connect() throws IOException;

    /**
     * Connects with this environment, which is added to the creation one.
     *
     * @throws IOException if it could not
     * @throws SecurityException if it was not allowed
     */
    void connect(Map<String, ?> env) throws IOException;

    /**
     * Where operations on the remote server go through.
     *
     * @throws IOException if it is not connected
     */
    MBeanServerConnection getMBeanServerConnection() throws IOException;

    /**
     * The same, acting on another's behalf. See the class note.
     *
     * @throws UnsupportedOperationException by default
     */
    default MBeanServerConnection getMBeanServerConnection(Subject delegationSubject)
        throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Closes. It can be called more than once.
     *
     * @throws IOException if something failed while closing
     */
    void close() throws IOException;

    /**
     * Registers a listener of the connection's state.
     *
     * <p>It can be done before connecting, and it has to be done that way not to miss the
     * {@code OPENED}.
     */
    void addConnectionNotificationListener(NotificationListener listener,
                                           NotificationFilter filter, Object handback);

    /**
     * Removes it, in all its combinations of filter and handback.
     *
     * @throws javax.management.ListenerNotFoundException if it was not there
     */
    void removeConnectionNotificationListener(NotificationListener listener)
        throws javax.management.ListenerNotFoundException;

    /**
     * Removes that exact combination.
     *
     * @throws javax.management.ListenerNotFoundException if it was not there
     */
    void removeConnectionNotificationListener(NotificationListener l, NotificationFilter f,
                                              Object handback)
        throws javax.management.ListenerNotFoundException;

    /**
     * This connection's identifier. See the class note.
     *
     * @throws IOException if it is not connected
     */
    String getConnectionId() throws IOException;
}
