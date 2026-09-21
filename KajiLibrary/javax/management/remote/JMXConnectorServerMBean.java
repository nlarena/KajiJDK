package javax.management.remote;

import java.io.IOException;
import java.util.Map;

/**
 * KajiLibrary's javax.management.remote.JMXConnectorServerMBean -- a connector server's MBean
 * face.
 *
 * <p>It exists so that the server can be <b>registered in the very MBean server it exposes</b>.
 * It sounds circular and it is useful: that way it can be started, stopped and queried through
 * the same paths as any other MBean, even from a remote console connected through it.
 *
 * <p>It is a standard MBean interface: the name ends in {@code MBean} and the class that fulfils
 * it is {@link JMXConnectorServer}, without the suffix. That convention is what makes the
 * registration work.
 */
public interface JMXConnectorServerMBean {

    /**
     * Starts listening.
     *
     * @throws IOException if it could not
     * @throws IllegalStateException if it was already stopped; a stopped server is not restarted
     */
    void start() throws IOException;

    /**
     * Stops listening and closes the open connections.
     *
     * <p>It is final: after this, {@link #start} fails.
     *
     * @throws IOException if something failed while closing
     */
    void stop() throws IOException;

    /** Whether it is listening. */
    boolean isActive();

    /** Chains an interceptor in front of the MBean server. See {@link MBeanServerForwarder}. */
    void setMBeanServerForwarder(MBeanServerForwarder mbsf);

    /** The identifiers of the open connections. */
    String[] getConnectionIds();

    /** The address where it listens, or null if it did not start. */
    JMXServiceURL getAddress();

    /** The environment it was created with, read-only. */
    Map<String, ?> getAttributes();

    /**
     * A client connector towards this same server.
     *
     * <p>It is what allows something in the same process to talk to the server through the remote
     * path, without shortcuts. It serves for testing.
     *
     * @throws UnsupportedOperationException if this server does not support it
     * @throws IllegalStateException if it is not active
     * @throws IOException if it could not
     */
    JMXConnector toJMXConnector(Map<String, ?> env) throws IOException;
}
