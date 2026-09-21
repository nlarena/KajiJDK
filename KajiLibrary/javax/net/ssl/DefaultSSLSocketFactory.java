package javax.net.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * The factory {@link SSLSocketFactory#getDefault} returns when no TLS is available.
 *
 * <p>Package-private, as in the JDK. It exists because {@code getDefault()} declares no exception:
 * instead of returning {@code null} --which would blow up much later and somewhere else-- it
 * returns this, which fails on the first attempt to use it and <strong>carries the original cause
 * inside</strong>. The error ends up showing where it can be understood.
 */
final class DefaultSSLSocketFactory extends SSLSocketFactory {

    private final Exception reason;

    DefaultSSLSocketFactory(Exception reason) {
        this.reason = reason;
    }

    private Socket fail() throws SocketException {
        throw new SocketException("no TLS support: " + this.reason.getMessage());
    }

    public Socket createSocket() throws IOException {
        return fail();
    }

    public Socket createSocket(String host, int port) throws IOException {
        return fail();
    }

    public Socket createSocket(Socket s, String host, int port, boolean autoClose)
            throws IOException {
        return fail();
    }

    public Socket createSocket(InetAddress address, int port) throws IOException {
        return fail();
    }

    public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort)
            throws IOException {
        return fail();
    }

    public Socket createSocket(InetAddress address, int port, InetAddress clientAddress,
            int clientPort) throws IOException {
        return fail();
    }

    /** Empty: no suite is available, which is the truth. */
    public String[] getDefaultCipherSuites() {
        return new String[0];
    }

    /** Empty, for the same reason. */
    public String[] getSupportedCipherSuites() {
        return new String[0];
    }
}
