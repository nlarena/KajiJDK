package javax.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * The factory {@code ServerSocketFactory.getDefault()} returns.
 *
 * <p>Package-private: it is not API. Each method is the {@link ServerSocket} constructor that
 * corresponds to it; see {@link DefaultSocketFactory}.
 */
final class DefaultServerSocketFactory extends ServerSocketFactory {

    /** An unbound one; this factory does know how. */
    @Override
    public ServerSocket createServerSocket() throws IOException {
        return new ServerSocket();
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        return new ServerSocket(port);
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog) throws IOException {
        return new ServerSocket(port, backlog);
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress)
        throws IOException {
        return new ServerSocket(port, backlog, ifAddress);
    }
}
