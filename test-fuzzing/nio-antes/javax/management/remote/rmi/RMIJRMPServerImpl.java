package javax.management.remote.rmi;

import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

import javax.security.auth.Subject;

/**
 * The JMX server reached over JRMP, RMI's own protocol.
 *
 * <h2>What it adds over {@link RMIServerImpl}</h2>
 *
 * <p>Only the transport. The class above already knows how to keep the client list, authenticate
 * and close in cascade; here are the port, the two socket factories and the three operations
 * that talk to RMI: exporting, giving the stub and unexporting.
 *
 * <h2>The socket factories</h2>
 *
 * <p>They are the point where TLS is put on a JMX connection. The client's travels
 * <strong>inside the stub</strong> --that is why it has to be serializable--: the client
 * receives the remote object and with it the instruction on what kind of socket to talk to it
 * with. The server's stays on this side and decides how listening is done.
 *
 * <h2>State in this VM</h2>
 *
 * <p>{@link #export} needs {@link UnicastRemoteObject#exportObject}, which this VM does not
 * have: publishing a remote object is opening a port and serving the protocol, and that is
 * transport. It throws {@link UnsupportedOperationException} with the reason, like all of
 * {@code java.rmi.server}.
 *
 * <p>{@link #toStub} throws {@link NoSuchObjectException}, which is exactly what the JDK does
 * when it is called without having exported. Here it was never exported, so that is always the
 * case, and no invented exception is needed to say so.
 *
 * <p>The rest --the protocol, building the client's connection, the close-- works. The connection
 * {@link #makeClient} returns is a real {@link RMIConnectionImpl}, which forwards to the
 * {@link javax.management.MBeanServer}; what there is not is a way of making it reach another
 * machine.
 *
 * @since 1.5
 */
public class RMIJRMPServerImpl extends RMIServerImpl {

    private final int port;
    private final RMIClientSocketFactory csf;
    private final RMIServerSocketFactory ssf;

    /**
     * A JRMP server on that port.
     *
     * @param port the port; {@code 0} lets the system pick it
     * @param csf the client's socket factory, or {@code null} for the usual one
     * @param ssf the server's socket factory, or {@code null} for the usual one
     * @param env the configuration properties, or {@code null}
     * @throws IOException if it could not be created
     * @throws IllegalArgumentException if the port is negative
     */
    public RMIJRMPServerImpl(int port, RMIClientSocketFactory csf, RMIServerSocketFactory ssf,
            Map<String, ?> env) throws IOException {
        super(env);
        if (port < 0) {
            throw new IllegalArgumentException("Negative port: " + port);
        }
        this.port = port;
        this.csf = csf;
        this.ssf = ssf;
    }

    /**
     * Publishes this object over RMI.
     *
     * @throws IOException if it could not be published
     * @throws UnsupportedOperationException in this VM, which has no RMI transport
     */
    @Override
    protected void export() throws IOException {
        // The two forms are told apart because the JDK tells them apart: with the default factories
        // `null` is not passed, the overload that does not take them is called. The difference
        // shows in the stub that reaches the client.
        if (csf == null && ssf == null) {
            UnicastRemoteObject.exportObject(this, port);
        } else {
            UnicastRemoteObject.exportObject(this, port, csf, ssf);
        }
    }

    /**
     * The protocol's name.
     *
     * @return {@code "rmi"}
     */
    @Override
    protected String getProtocol() {
        return "rmi";
    }

    /**
     * The remote object that has to be sent to the client.
     *
     * @return the stub
     * @throws NoSuchObjectException if this server is not exported, which here is always
     */
    @Override
    public Remote toStub() throws IOException {
        throw new NoSuchObjectException("object not exported");
    }

    /**
     * Builds a client's connection and publishes it.
     *
     * <p>In the JDK the connection is exported too, because the client is going to call it directly
     * and not through this server. Here it is created all the same --it is an
     * {@link RMIConnectionImpl} that works-- but it is not exported: there is no transport, and
     * exporting would be the only thing that would fail of an object that otherwise runs.
     *
     * @param connectionId the identifier it gets
     * @param subject who authenticated, or {@code null}
     * @return the connection
     * @throws IOException if it could not be created
     */
    @Override
    protected RMIConnection makeClient(String connectionId, Subject subject) throws IOException {
        if (connectionId == null) {
            throw new NullPointerException("Null connectionId");
        }
        return new RMIConnectionImpl(this, connectionId, getDefaultClassLoader(), subject,
                environment());
    }

    /**
     * Stops publishing a client's connection.
     *
     * <p>In the JDK this unexports it, which is what makes it unreachable from outside. Here there
     * is nothing to undo, because {@link #makeClient} never exported it.
     *
     * @param client the connection
     * @throws IOException if it could not be closed
     * @throws NullPointerException if {@code client} is {@code null}
     */
    @Override
    protected void closeClient(RMIConnection client) throws IOException {
        if (client == null) {
            throw new NullPointerException("Null client");
        }
    }

    /**
     * Stops publishing this server.
     *
     * @throws IOException if it could not be closed
     */
    @Override
    protected void closeServer() throws IOException {
    }
}
