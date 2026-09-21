package javax.rmi.ssl;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * The factory that makes the server side of an RMI call listen over SSL.
 *
 * <p>It is the counterpart of {@link SslRMIClientSocketFactory}, and unlike that one it **does**
 * have state: the suites, the protocols and the demand for a client certificate are decisions of
 * the server, are configured here and travel nowhere.
 *
 * <h2>Why it validates in the constructor</h2>
 *
 * <p>The constructor makes a test {@link SSLSocket} only in order to check that the suites and the
 * protocols it was passed exist. It is on purpose: if it did not, a badly written name would not
 * show until the first incoming connection --and there the error appears in the thread of the
 * `accept`, with no visible relation to the line that configured it wrongly.
 *
 * <h2>About `equals`</h2>
 *
 * <p>Two factories are equal if they have the **same class** and the same configuration. The class
 * counts because a subclass can add state this class does not know how to compare, and two "equal"
 * factories make RMI share one listening socket between exported objects.
 */
public class SslRMIServerSocketFactory implements RMIServerSocketFactory {

    private static SSLSocketFactory defaultSSLSocketFactory = null;

    private final String[] enabledCipherSuites;
    private final String[] enabledProtocols;
    private final boolean needClientAuth;
    private List<String> enabledCipherSuitesList;
    private List<String> enabledProtocolsList;
    private SSLContext context;

    /**
     * A factory with the usual SSL configuration and demanding no client certificate.
     */
    public SslRMIServerSocketFactory() {
        this(null, null, null, false);
    }

    /**
     * A factory with the given suites and protocols.
     *
     * @param enabledCipherSuites the suites to enable, or `null` for the usual ones
     * @param enabledProtocols the protocols to enable, or `null` for the usual ones
     * @param needClientAuth whether a certificate is demanded of the client
     * @throws IllegalArgumentException if some suite or protocol is not supported, or if it could
     *     not be found out
     */
    public SslRMIServerSocketFactory(String[] enabledCipherSuites, String[] enabledProtocols,
            boolean needClientAuth) throws IllegalArgumentException {
        this(null, enabledCipherSuites, enabledProtocols, needClientAuth);
    }

    /**
     * A factory that takes its sockets from that context.
     *
     * @param context the SSL context, or `null` for the usual one
     * @param enabledCipherSuites the suites to enable, or `null` for the usual ones
     * @param enabledProtocols the protocols to enable, or `null` for the usual ones
     * @param needClientAuth whether a certificate is demanded of the client
     * @throws IllegalArgumentException if some suite or protocol is not supported, or if it could
     *     not be found out
     */
    public SslRMIServerSocketFactory(SSLContext context, String[] enabledCipherSuites,
            String[] enabledProtocols, boolean needClientAuth) throws IllegalArgumentException {
        this.context = context;
        this.enabledCipherSuites = enabledCipherSuites == null ? null : enabledCipherSuites.clone();
        this.enabledProtocols = enabledProtocols == null ? null : enabledProtocols.clone();
        this.needClientAuth = needClientAuth;

        if (this.enabledCipherSuites == null && this.enabledProtocols == null) {
            return;
        }

        // The lists are for `equals`: comparing arrays by contents by hand at every call is what
        // this avoids.
        if (this.enabledCipherSuites != null) {
            this.enabledCipherSuitesList = new ArrayList<String>(
                    Arrays.asList(this.enabledCipherSuites));
        }
        if (this.enabledProtocols != null) {
            this.enabledProtocolsList = new ArrayList<String>(
                    Arrays.asList(this.enabledProtocols));
        }

        SSLSocket probe;
        try {
            probe = (SSLSocket) factory().createSocket();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to check if the cipher suites and protocols to enable are supported", e);
        }
        if (this.enabledCipherSuites != null) {
            probe.setEnabledCipherSuites(this.enabledCipherSuites);
        }
        if (this.enabledProtocols != null) {
            probe.setEnabledProtocols(this.enabledProtocols);
        }
    }

    /** The enabled suites, or `null` if they are the usual ones. */
    public final String[] getEnabledCipherSuites() {
        return this.enabledCipherSuites == null ? null : this.enabledCipherSuites.clone();
    }

    /** The enabled protocols, or `null` if they are the usual ones. */
    public final String[] getEnabledProtocols() {
        return this.enabledProtocols == null ? null : this.enabledProtocols.clone();
    }

    /** Whether a certificate is demanded of the client. */
    public final boolean getNeedClientAuth() {
        return this.needClientAuth;
    }

    /**
     * A listening socket that negotiates SSL at every `accept`.
     *
     * <p>The handshake does not happen here but in the `accept`: creating the listening socket
     * talks to nobody.
     *
     * @throws IOException if it cannot listen on that port
     */
    public ServerSocket createServerSocket(int port) throws IOException {
        return new SslServerSocket(port, factory(), this.enabledCipherSuites,
                this.enabledProtocols, this.needClientAuth);
    }

    /**
     * Two factories of the same class with the same configuration.
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!obj.getClass().equals(this.getClass())) {
            return false;
        }
        return checkParameters((SslRMIServerSocketFactory) obj);
    }

    /** The comparison of configuration {@link #equals} uses. */
    private boolean checkParameters(SslRMIServerSocketFactory that) {
        if (this.needClientAuth != that.needClientAuth) {
            return false;
        }
        if (this.context != that.context) {
            return false;
        }
        if (this.enabledCipherSuites == null ? that.enabledCipherSuites != null
                : !this.enabledCipherSuitesList.equals(that.enabledCipherSuitesList)) {
            return false;
        }
        if (this.enabledProtocols == null ? that.enabledProtocols != null
                : !this.enabledProtocolsList.equals(that.enabledProtocolsList)) {
            return false;
        }
        return true;
    }

    /** Coherent with {@link #equals}. */
    public int hashCode() {
        return this.getClass().hashCode()
                + (this.needClientAuth ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode())
                + (this.enabledCipherSuites == null ? 0 : this.enabledCipherSuitesList.hashCode())
                + (this.enabledProtocols == null ? 0 : this.enabledProtocolsList.hashCode());
    }

    /** The socket factory: the context's if there is one, the usual one if not. */
    private SSLSocketFactory factory() {
        return this.context == null ? getDefaultSSLSocketFactory() : this.context.getSocketFactory();
    }

    /** The usual SSL factory, memoised; building it is not free. */
    private static synchronized SSLSocketFactory getDefaultSSLSocketFactory() {
        if (defaultSSLSocketFactory == null) {
            defaultSSLSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
        return defaultSSLSocketFactory;
    }
}
