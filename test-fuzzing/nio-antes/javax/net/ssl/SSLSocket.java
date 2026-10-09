package javax.net.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.function.BiFunction;

/**
 * A {@link Socket} that encrypts.
 *
 * <h2>The promise, and what has to be done for it to be true</h2>
 *
 * <p>Everything about {@code Socket} still holds: it is read and written the same, and the
 * encryption happens underneath. That is what allows taking code that talked in the clear and
 * making it secure by changing who creates the socket.
 *
 * <p>With one caveat that costs dearly: <strong>by default the server's identity is not
 * checked</strong>. A freshly created socket encrypts against anybody, including a middleman with a
 * legitimate certificate for another domain. Turning that check on is setting
 * {@link SSLParameters#setEndpointIdentificationAlgorithm} to {@code "HTTPS"}. Not doing it is the
 * most frequent way of having TLS that protects against nothing.
 *
 * <h2>When the handshake happens</h2>
 *
 * <p>Not when creating the socket: on the first read or write, or when {@link #startHandshake} asks
 * for it. That is why a certificate error does not show where one expects it but on the first
 * {@code read} — and why it is worth calling {@code startHandshake} explicitly when one wants to
 * fail early.
 */
public abstract class SSLSocket extends Socket {

    /** Unconnected. */
    protected SSLSocket() {
        super();
    }

    /** Connected to a host by name. */
    protected SSLSocket(String host, int port) throws IOException, UnknownHostException {
        super(host, port);
    }

    /** Connected to an address. */
    protected SSLSocket(InetAddress address, int port) throws IOException {
        super(address, port);
    }

    /** Connected, also binding a local address. */
    protected SSLSocket(String host, int port, InetAddress clientAddress, int clientPort)
            throws IOException, UnknownHostException {
        super(host, port, clientAddress, clientPort);
    }

    /** The same, with the remote address already resolved. */
    protected SSLSocket(InetAddress address, int port, InetAddress clientAddress, int clientPort)
            throws IOException {
        super(address, port, clientAddress, clientPort);
    }

    /** All the suites this socket knows. */
    public abstract String[] getSupportedCipherSuites();

    /** The ones enabled now. */
    public abstract String[] getEnabledCipherSuites();

    /** Sets the enabled suites. */
    public abstract void setEnabledCipherSuites(String[] suites);

    /** All the protocols it knows. */
    public abstract String[] getSupportedProtocols();

    /** The ones enabled now. */
    public abstract String[] getEnabledProtocols();

    /** Sets the enabled protocols. */
    public abstract void setEnabledProtocols(String[] protocols);

    /**
     * The session, forcing the handshake if it did not happen yet.
     *
     * <p>It blocks, and if the handshake fails <strong>it does not throw</strong>: it returns an
     * invalid session with suite {@code SSL_NULL_WITH_NULL_NULL}. It is an old signature that could
     * not declare an exception, and the trap is that the error only shows if one looks at the
     * suite.
     */
    public abstract SSLSession getSession();

    /** The session being negotiated, or {@code null} if there is no handshake in progress. */
    public SSLSession getHandshakeSession() {
        throw new UnsupportedOperationException("socket exposes no handshake session");
    }

    /** Adds somebody to find out about each finished handshake. */
    public abstract void addHandshakeCompletedListener(HandshakeCompletedListener listener);

    /** Removes a listener. */
    public abstract void removeHandshakeCompletedListener(HandshakeCompletedListener listener);

    /**
     * Forces the handshake, or renegotiates if there already was one.
     *
     * @throws IOException if the handshake fails — unlike {@link #getSession}, here the error does
     *     arrive as an exception, which is the reason to call it explicitly
     */
    public abstract void startHandshake() throws IOException;

    /**
     * Whether this socket is the client.
     *
     * @throws IllegalArgumentException if the handshake already started
     */
    public abstract void setUseClientMode(boolean mode);

    /** Whether it is the client. */
    public abstract boolean getUseClientMode();

    /** Requires client authentication; server side only. */
    public abstract void setNeedClientAuth(boolean need);

    /** Whether it is required. */
    public abstract boolean getNeedClientAuth();

    /** Requests client authentication without requiring it. */
    public abstract void setWantClientAuth(boolean want);

    /** Whether it is requested. */
    public abstract boolean getWantClientAuth();

    /** Whether new sessions can be created. */
    public abstract void setEnableSessionCreation(boolean flag);

    /** Whether new sessions can be created. */
    public abstract boolean getEnableSessionCreation();

    /** All the configuration together. */
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

    /** The application protocol agreed by ALPN. */
    public String getApplicationProtocol() {
        throw new UnsupportedOperationException("this socket does not support ALPN");
    }

    /** The one being agreed during the handshake. */
    public String getHandshakeApplicationProtocol() {
        throw new UnsupportedOperationException("this socket does not support ALPN");
    }

    /** Chooses the application protocol with a function of its own. */
    public void setHandshakeApplicationProtocolSelector(
            BiFunction<SSLSocket, List<String>, String> selector) {
        throw new UnsupportedOperationException("this socket does not support ALPN");
    }

    /** The selector set, or {@code null}. */
    public BiFunction<SSLSocket, List<String>, String> getHandshakeApplicationProtocolSelector() {
        throw new UnsupportedOperationException("this socket does not support ALPN");
    }
}
