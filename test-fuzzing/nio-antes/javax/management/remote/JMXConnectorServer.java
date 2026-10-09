package javax.management.remote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.remote.JMXConnectorServer -- the base of connector servers.
 *
 * <p>It joins three roles, and the interface list says so: it is a
 * {@link JMXConnectorServerMBean} so as to be registrable, an {@link MBeanRegistration} to learn
 * when it is registered, and a {@link NotificationBroadcasterSupport} to report connections.
 *
 * <h2>The MBean server can come from two sides</h2>
 *
 * <p>Through the constructor, or through registration: if it is built without one and then
 * registered as an MBean, {@link #preRegister} takes the server it was registered in. It is what
 * allows writing "register this connector" in a configuration without naming the server.
 *
 * <h2>{@link #setMBeanServerForwarder} stacks backwards</h2>
 *
 * <p>Each call puts the new interceptor <b>in front</b> of what was already there, so the last
 * one added is the first to see the calls. See {@link MBeanServerForwarder}.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This class is whole: what this library lacks is a <b>protocol</b>, and that lives in the
 * subclasses a provider brings. See {@link JMXConnectorServerFactory}.
 */
public abstract class JMXConnectorServer extends NotificationBroadcasterSupport
    implements JMXConnectorServerMBean, MBeanRegistration, JMXAddressable {

    /** The environment key where the {@link JMXAuthenticator} goes. */
    public static final String AUTHENTICATOR = "jmx.remote.authenticator";

    /** Which MBean server it exposes. */
    private MBeanServer mbeanServer = null;

    /** Under what name it was registered, or null. */
    private ObjectName myName;

    /** The open connections. */
    private final ArrayList<String> connectionIds = new ArrayList<String>();

    /** The notifications' next sequence number. */
    private long sequenceNumber = 0;

    /** Without an MBean server; it is taken when registering it. */
    public JMXConnectorServer() {
        this(null);
    }

    /** @param mbeanServer which server it exposes, or null */
    public JMXConnectorServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    /** Which MBean server it exposes. */
    public synchronized MBeanServer getMBeanServer() {
        return this.mbeanServer;
    }

    /**
     * Chains an interceptor in front. See the class note on the order.
     *
     * @throws IllegalArgumentException if it is null
     */
    public synchronized void setMBeanServerForwarder(MBeanServerForwarder mbsf) {
        if (mbsf == null) {
            throw new IllegalArgumentException("Invalid null argument: mbsf");
        }
        if (this.mbeanServer != null) {
            mbsf.setMBeanServer(this.mbeanServer);
        }
        this.mbeanServer = mbsf;
    }

    /** The identifiers of the open connections. */
    public String[] getConnectionIds() {
        synchronized (this.connectionIds) {
            return this.connectionIds.toArray(new String[this.connectionIds.size()]);
        }
    }

    /**
     * A client connector towards this server.
     *
     * <p>It goes through {@link JMXConnectorFactory} with its own address, without shortcuts: that
     * is what makes it serve to really exercise the remote path.
     *
     * @throws IllegalStateException if it is not active
     * @throws IOException if it could not
     */
    public JMXConnector toJMXConnector(Map<String, ?> env) throws IOException {
        if (!isActive()) {
            throw new IllegalStateException("Connector server is not active");
        }
        JMXServiceURL address = getAddress();
        if (address == null) {
            throw new UnsupportedOperationException(
                "This connector server does not support connections from a JMXConnector");
        }
        return JMXConnectorFactory.newJMXConnector(address, env);
    }

    /** The three connection notifications this class emits. */
    public MBeanNotificationInfo[] getNotificationInfo() {
        final String[] types = {
            JMXConnectionNotification.OPENED,
            JMXConnectionNotification.CLOSED,
            JMXConnectionNotification.FAILED,
        };
        final String className = JMXConnectionNotification.class.getName();
        final String description = "A client connection has been opened or closed";
        return new MBeanNotificationInfo[] {
            new MBeanNotificationInfo(types, className, description),
        };
    }

    /** For the subclass to report that a connection was opened. */
    protected void connectionOpened(String connectionId, String message, Object userData) {
        synchronized (this.connectionIds) {
            this.connectionIds.add(connectionId);
        }
        sendNotification(JMXConnectionNotification.OPENED, connectionId, message, userData);
    }

    /** The same, closed in an orderly way. */
    protected void connectionClosed(String connectionId, String message, Object userData) {
        synchronized (this.connectionIds) {
            this.connectionIds.remove(connectionId);
        }
        sendNotification(JMXConnectionNotification.CLOSED, connectionId, message, userData);
    }

    /** The same, cut by itself. */
    protected void connectionFailed(String connectionId, String message, Object userData) {
        synchronized (this.connectionIds) {
            this.connectionIds.remove(connectionId);
        }
        sendNotification(JMXConnectionNotification.FAILED, connectionId, message, userData);
    }

    /**
     * Takes the server it is registered in, if it had none.
     *
     * <p>See the class note. It only takes it the first time: registering it twice does not move
     * it.
     *
     * @throws NullPointerException if the server or the name are null
     */
    public synchronized ObjectName preRegister(MBeanServer mbs, ObjectName name) {
        if (mbs == null || name == null) {
            throw new NullPointerException("Null MBeanServer or ObjectName");
        }
        if (this.mbeanServer == null) {
            this.mbeanServer = mbs;
            this.myName = name;
        }
        return name;
    }

    /** It does nothing. */
    public void postRegister(Boolean registrationDone) {
    }

    /**
     * Stops it before taking it out of the registry.
     *
     * <p>It is what avoids leaving a port listening after unregistering the MBean.
     *
     * @throws IOException if it failed while stopping
     */
    public synchronized void preDeregister() throws Exception {
        if (this.myName != null && isActive()) {
            stop();
            this.myName = null;
        }
    }

    /** It forgets the name. */
    public void postDeregister() {
        this.myName = null;
    }

    /** The common building of the three connection notifications. */
    private void sendNotification(String type, String connectionId, String message,
                                  Object userData) {
        long seq;
        synchronized (this) {
            seq = this.sequenceNumber;
            this.sequenceNumber = this.sequenceNumber + 1;
        }
        sendNotification(new JMXConnectionNotification(type, getNotificationSource(), connectionId,
                                                       seq, message, userData));
    }

    /**
     * Who appears as the notifications' source.
     *
     * <p>The name it was registered under if there is one, and the object otherwise. Putting the
     * name is the right thing when the notifications cross the network: the object does not travel,
     * the name does.
     */
    private Object getNotificationSource() {
        if (this.myName != null) {
            return this.myName;
        }
        return this;
    }
}
