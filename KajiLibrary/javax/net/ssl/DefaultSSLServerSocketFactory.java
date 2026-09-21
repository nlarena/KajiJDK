package javax.net.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;

/**
 * The server-side counterpart of {@link DefaultSSLSocketFactory}: it fails when used, with the
 * original cause inside.
 */
final class DefaultSSLServerSocketFactory extends SSLServerSocketFactory {

    private final Exception reason;

    DefaultSSLServerSocketFactory(Exception reason) {
        this.reason = reason;
    }

    private ServerSocket fail() throws SocketException {
        throw new SocketException("no TLS support: " + this.reason.getMessage());
    }

    public ServerSocket createServerSocket() throws IOException {
        return fail();
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        return fail();
    }

    public ServerSocket createServerSocket(int port, int backlog) throws IOException {
        return fail();
    }

    public ServerSocket createServerSocket(int port, int backlog, InetAddress address)
            throws IOException {
        return fail();
    }

    /** Empty: no suite is available. */
    public String[] getDefaultCipherSuites() {
        return new String[0];
    }

    /** Empty, for the same reason. */
    public String[] getSupportedCipherSuites() {
        return new String[0];
    }
}
