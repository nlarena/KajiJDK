package javax.rmi.ssl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * The listening socket {@link SslRMIServerSocketFactory#createServerSocket(int)} returns.
 *
 * <p>It is not a class of the JDK: there it is an anonymous class inside the `createServerSocket`.
 * Here it has a name and is package-private, which for the purposes of the API is the same --nobody
 * outside `javax.rmi.ssl` can name it-- and it also reads better.
 *
 * <p>The only thing it does is wrap each accepted connection in an {@link SSLSocket} in server
 * mode. The TCP is accepted by {@link ServerSocket}; the TLS starts when somebody reads or writes.
 */
final class SslServerSocket extends ServerSocket {

    private final SSLSocketFactory factory;
    private final String[] suites;
    private final String[] protocols;
    private final boolean needClientAuth;

    SslServerSocket(int port, SSLSocketFactory factory, String[] suites, String[] protocols,
            boolean needClientAuth) throws IOException {
        super(port);
        this.factory = factory;
        this.suites = suites;
        this.protocols = protocols;
        this.needClientAuth = needClientAuth;
    }

    /**
     * It accepts a connection and wraps it in SSL.
     *
     * <p>The TCP socket is handed to the {@link SSLSocketFactory} with `autoClose` at `true`:
     * closing the SSL socket has to close the one underneath as well, or the connection is left
     * half released.
     */
    public Socket accept() throws IOException {
        Socket plain = super.accept();
        SSLSocket secure = (SSLSocket) this.factory.createSocket(
                plain, plain.getInetAddress().getHostName(), plain.getPort(), true);
        secure.setUseClientMode(false);
        if (this.suites != null) {
            secure.setEnabledCipherSuites(this.suites);
        }
        if (this.protocols != null) {
            secure.setEnabledProtocols(this.protocols);
        }
        secure.setNeedClientAuth(this.needClientAuth);
        return secure;
    }
}
