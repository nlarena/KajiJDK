package javax.net.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * La fabrica que devuelve {@link SSLSocketFactory#getDefault} cuando no hay TLS disponible.
 *
 * <p>De paquete, igual que en el JDK. Existe porque {@code getDefault()} no declara excepcion: en
 * vez de devolver {@code null} —que estallaria mucho despues y en otro lado— devuelve esto, que
 * falla en el primer intento de uso y <strong>lleva adentro la causa original</strong>. El error
 * termina apareciendo donde se lo puede entender.
 */
final class DefaultSSLSocketFactory extends SSLSocketFactory {

    private final Exception motivo;

    DefaultSSLSocketFactory(Exception motivo) {
        this.motivo = motivo;
    }

    private Socket fallar() throws SocketException {
        throw new SocketException("no hay soporte de TLS: " + this.motivo.getMessage());
    }

    public Socket createSocket() throws IOException {
        return fallar();
    }

    public Socket createSocket(String host, int port) throws IOException {
        return fallar();
    }

    public Socket createSocket(Socket s, String host, int port, boolean autoClose)
            throws IOException {
        return fallar();
    }

    public Socket createSocket(InetAddress address, int port) throws IOException {
        return fallar();
    }

    public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort)
            throws IOException {
        return fallar();
    }

    public Socket createSocket(InetAddress address, int port, InetAddress clientAddress,
            int clientPort) throws IOException {
        return fallar();
    }

    /** Vacio: no hay ninguna suite disponible, que es la verdad. */
    public String[] getDefaultCipherSuites() {
        return new String[0];
    }

    /** Vacio, por lo mismo. */
    public String[] getSupportedCipherSuites() {
        return new String[0];
    }
}
