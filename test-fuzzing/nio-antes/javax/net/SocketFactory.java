package javax.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * KajiLibrary's javax.net.SocketFactory -- creates client sockets.
 *
 * <p>A layer of indirection over {@code new Socket(...)}, and its whole reason for being is what it
 * allows: the same client code talking in the clear or over TLS by changing <b>only</b> the
 * factory. {@code javax.net.ssl.SSLSocketFactory} is a subclass, and that is the point.
 *
 * <p>It also serves to slip in a proxy, an instrumented socket, or a fake one for tests.
 *
 * <h2>{@link #createSocket()} without arguments</h2>
 *
 * <p>It returns an <b>unconnected</b> socket. It is the only way of setting options that have to be
 * set <b>before</b> connecting --the buffer sizes, {@code SO_REUSEADDR}-- and that is why it is not
 * abstract: the base class implements it by throwing {@link java.net.SocketException}, and a
 * factory that knows how overrides it.
 *
 * <p>The two with a local address exist to choose which interface to go out through, which matters
 * on a machine with several. (The note said "the four"; two of the four connecting methods take a
 * local address.)
 */
public abstract class SocketFactory {

    /** The usual one; it is created only once. */
    private static SocketFactory theFactory;

    /** For subclasses. */
    protected SocketFactory() {
    }

    /**
     * The default factory: the one that creates ordinary, unencrypted sockets.
     *
     * <p>Always the same instance.
     */
    public static SocketFactory getDefault() {
        synchronized (SocketFactory.class) {
            if (theFactory == null) {
                theFactory = new DefaultSocketFactory();
            }
            return theFactory;
        }
    }

    /**
     * An unconnected socket. See the class note.
     *
     * @throws IOException if this factory cannot create unconnected sockets
     */
    public Socket createSocket() throws IOException {
        throw new java.net.SocketException("Unconnected sockets not implemented");
    }

    /**
     * Connects to that host and port.
     *
     * @throws IOException if it could not connect
     * @throws UnknownHostException if the name does not resolve
     */
    public abstract Socket createSocket(String host, int port)
        throws IOException, UnknownHostException;

    /**
     * Likewise, going out through that local address and port. See the class note.
     *
     * @throws IOException if it could not connect
     * @throws UnknownHostException if the name does not resolve
     */
    public abstract Socket createSocket(String host, int port, InetAddress localHost,
                                        int localPort) throws IOException, UnknownHostException;

    /**
     * Connects to that address and port.
     *
     * @throws IOException if it could not connect
     */
    public abstract Socket createSocket(InetAddress host, int port) throws IOException;

    /**
     * Likewise, going out through that local address and port.
     *
     * @throws IOException if it could not connect
     */
    public abstract Socket createSocket(InetAddress address, int port, InetAddress localAddress,
                                        int localPort) throws IOException;
}
