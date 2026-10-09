package javax.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * The factory {@code SocketFactory.getDefault()} returns.
 *
 * <p>Package-private: it is not API. Each method is literally the {@link Socket} constructor that
 * corresponds to it -- which is right, because the point of {@link SocketFactory} is that the
 * default factory adds nothing.
 */
final class DefaultSocketFactory extends SocketFactory {

    /** An unconnected socket; this factory does know how. */
    @Override
    public Socket createSocket() {
        return new Socket();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return new Socket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort)
        throws IOException {
        return new Socket(host, port, clientAddress, clientPort);
    }

    @Override
    public Socket createSocket(InetAddress address, int port) throws IOException {
        return new Socket(address, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress clientAddress,
                               int clientPort) throws IOException {
        return new Socket(address, port, clientAddress, clientPort);
    }
}
