package javax.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * La fabrica que devuelve {@code SocketFactory.getDefault()}.
 *
 * <p>De acceso de paquete: no es API. Cada metodo es literalmente el constructor de {@link Socket} que
 * le corresponde -- que es lo correcto, porque el sentido de {@link SocketFactory} es que la fabrica
 * por omision no agregue nada.
 */
final class DefaultSocketFactory extends SocketFactory {

    /** Un socket sin conectar; esta fabrica si sabe. */
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
