package javax.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * La fabrica que devuelve {@code ServerSocketFactory.getDefault()}.
 *
 * <p>De acceso de paquete: no es API. Cada metodo es el constructor de {@link ServerSocket} que le
 * corresponde; ver {@link DefaultSocketFactory}.
 */
final class DefaultServerSocketFactory extends ServerSocketFactory {

    /** Uno sin atar; esta fabrica si sabe. */
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
