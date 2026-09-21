package javax.net.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * A {@link ServerSocket} whose {@code accept}s return {@link SSLSocket}s.
 *
 * <h2>Why configure it here and not on each accepted socket</h2>
 *
 * <p>Because what is set on this object are the <strong>defaults</strong> of everything it accepts
 * from then on. A server that requires a client certificate says so once, and not on each
 * connection — where it would also be too late: the configuration has to be in place before the
 * handshake starts, and the handshake starts by itself.
 *
 * <p>The same setters exist on {@link SSLSocket} for the opposite case: changing something on
 * <em>one</em> connection without touching the rest.
 */
public abstract class SSLServerSocket extends ServerSocket {

    /** Unbound. */
    protected SSLServerSocket() throws IOException {
        super();
    }

    /** Listening on a port. */
    protected SSLServerSocket(int port) throws IOException {
        super(port);
    }

    /** With the number of pending connections. */
    protected SSLServerSocket(int port, int backlog) throws IOException {
        super(port, backlog);
    }

    /** Also bound to a concrete local address. */
    protected SSLServerSocket(int port, int backlog, InetAddress address) throws IOException {
        super(port, backlog, address);
    }

    /** The suites enabled by default for what is accepted. */
    public abstract String[] getEnabledCipherSuites();

    /** Sets the enabled suites. */
    public abstract void setEnabledCipherSuites(String[] suites);

    /** All the suites known. */
    public abstract String[] getSupportedCipherSuites();

    /** All the protocols known. */
    public abstract String[] getSupportedProtocols();

    /** The protocols enabled by default. */
    public abstract String[] getEnabledProtocols();

    /** Sets the enabled protocols. */
    public abstract void setEnabledProtocols(String[] protocols);

    /** Requires client authentication on what is accepted. */
    public abstract void setNeedClientAuth(boolean need);

    /** Whether it is required. */
    public abstract boolean getNeedClientAuth();

    /** Requests it without requiring it. */
    public abstract void setWantClientAuth(boolean want);

    /** Whether it is requested. */
    public abstract boolean getWantClientAuth();

    /**
     * Whether the accepted sockets act as client.
     *
     * <p>It sounds contradictory and it is not: in TLS the handshake role does not have to match
     * who opened the TCP connection. There are protocols where the accepting side is the TLS
     * client.
     */
    public abstract void setUseClientMode(boolean mode);

    /** Whether the accepted ones act as client. */
    public abstract boolean getUseClientMode();

    /** Whether new sessions can be created. */
    public abstract void setEnableSessionCreation(boolean flag);

    /** Whether new sessions can be created. */
    public abstract boolean getEnableSessionCreation();

    /** All the default configuration, together. */
    public SSLParameters getSSLParameters() {
        SSLParameters p = new SSLParameters();
        p.setCipherSuites(getEnabledCipherSuites());
        p.setProtocols(getEnabledProtocols());
        if (getNeedClientAuth()) {
            p.setNeedClientAuth(true);
        } else if (getWantClientAuth()) {
            p.setWantClientAuth(true);
        }
        return p;
    }

    /** Applies the configuration; only what is not {@code null}. */
    public void setSSLParameters(SSLParameters params) {
        String[] s = params.getCipherSuites();
        if (s != null) {
            setEnabledCipherSuites(s);
        }
        s = params.getProtocols();
        if (s != null) {
            setEnabledProtocols(s);
        }
        if (params.getNeedClientAuth()) {
            setNeedClientAuth(true);
        } else if (params.getWantClientAuth()) {
            setWantClientAuth(true);
        } else {
            setWantClientAuth(false);
        }
    }
}
