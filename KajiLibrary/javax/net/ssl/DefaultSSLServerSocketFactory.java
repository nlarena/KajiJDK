package javax.net.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;

/**
 * La contraparte de {@link DefaultSSLSocketFactory} del lado servidor: falla al usarse, con la
 * causa original adentro.
 */
final class DefaultSSLServerSocketFactory extends SSLServerSocketFactory {

    private final Exception motivo;

    DefaultSSLServerSocketFactory(Exception motivo) {
        this.motivo = motivo;
    }

    private ServerSocket fallar() throws SocketException {
        throw new SocketException("no hay soporte de TLS: " + this.motivo.getMessage());
    }

    public ServerSocket createServerSocket() throws IOException {
        return fallar();
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        return fallar();
    }

    public ServerSocket createServerSocket(int port, int backlog) throws IOException {
        return fallar();
    }

    public ServerSocket createServerSocket(int port, int backlog, InetAddress address)
            throws IOException {
        return fallar();
    }

    /** Vacio: no hay ninguna suite disponible. */
    public String[] getDefaultCipherSuites() {
        return new String[0];
    }

    /** Vacio, por lo mismo. */
    public String[] getSupportedCipherSuites() {
        return new String[0];
    }
}
