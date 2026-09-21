package javax.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * KajiLibrary's javax.net.ServerSocketFactory -- creates server sockets.
 *
 * <p>The mirror of {@link SocketFactory} for the listening side, with the same reason for being:
 * {@code javax.net.ssl.SSLServerSocketFactory} is a subclass, and swapping it is all it takes for a
 * server to start talking TLS.
 *
 * <h2>The three arguments</h2>
 *
 * <p>The port, the <b>backlog</b>, and the local address:
 *
 * <ul>
 *   <li>port 0 means "whichever the system wants", and afterwards one asks which it got;
 *   <li>the backlog is how many connections can be left waiting for somebody to accept them. Too
 *       small, and clients see the connection refused under a peak;
 *   <li>the local address decides <b>on which interface</b> it listens. Without it it listens on
 *       all of them, which on a machine with one leg on the internet is not always what is wanted.
 * </ul>
 *
 * <p>{@link #createServerSocket()} returns an unbound one, so that options can be set first; see
 * {@link SocketFactory#createSocket()}.
 */
public abstract class ServerSocketFactory {

    /** The usual one; it is created only once. */
    private static ServerSocketFactory theFactory;

    /** For subclasses. */
    protected ServerSocketFactory() {
    }

    /** The default factory: ordinary, unencrypted server sockets. Always the same one. */
    public static ServerSocketFactory getDefault() {
        synchronized (ServerSocketFactory.class) {
            if (theFactory == null) {
                theFactory = new DefaultServerSocketFactory();
            }
            return theFactory;
        }
    }

    /**
     * An unbound server socket. See the class note.
     *
     * @throws IOException if this factory cannot create them unbound
     */
    public ServerSocket createServerSocket() throws IOException {
        throw new java.net.SocketException("Unbound server sockets not implemented");
    }

    /**
     * Listens on that port; 0 lets the system choose.
     *
     * @throws IOException if it could not be opened
     */
    public abstract ServerSocket createServerSocket(int port) throws IOException;

    /**
     * Likewise, with that backlog. See the class note.
     *
     * @throws IOException if it could not be opened
     */
    public abstract ServerSocket createServerSocket(int port, int backlog) throws IOException;

    /**
     * Likewise, listening only on that interface. See the class note.
     *
     * @throws IOException if it could not be opened
     */
    public abstract ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress)
        throws IOException;
}
