package javax.net.ssl;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.BiFunction;

/**
 * TLS without transport: a state machine that translates between application bytes and network
 * bytes.
 *
 * <h2>Why it exists if there is already {@link SSLSocket}</h2>
 *
 * <p>Because an {@code SSLSocket} decides for you how reading and writing are done — blocking, one
 * thread per connection. A server with many connections cannot pay for that, and wants to multiplex
 * with a selector; another may want TLS over something that is not even TCP.
 *
 * <p>This engine separates the two things: <strong>it does not touch the network</strong>. It is
 * given buffers and returns buffers; whoever uses it decides how they travel. It is power in
 * exchange for responsibility, and that is why using it correctly is notoriously delicate.
 *
 * <h2>The loop, which is the only thing to understand</h2>
 *
 * <p>Each call returns an {@link SSLEngineResult} with two statuses, and the caller has to
 * <strong>obey them</strong>:
 *
 * <ul>
 * <li>{@code NEED_WRAP} — the engine has something to send; call {@link #wrap};</li>
 * <li>{@code NEED_UNWRAP} — it needs data; read from the network and call {@link #unwrap};</li>
 * <li>{@code NEED_TASK} — there is heavy work pending. Take it with {@link #getDelegatedTask} and
 *     <strong>run it</strong>. Ignoring this is the most common error: the handshake stays still
 *     forever and no exception says so;</li>
 * <li>{@code BUFFER_OVERFLOW} / {@code BUFFER_UNDERFLOW} — they are not failures: they are requests
 *     for more room or more data. Size with {@link SSLSession#getPacketBufferSize} and
 *     {@link SSLSession#getApplicationBufferSize} and retry.</li>
 * </ul>
 *
 * <h2>The two directions are closed separately</h2>
 *
 * <p>{@link #closeOutbound} and {@link #closeInbound} are different on purpose: TLS closes each
 * direction with its own alert, and closing the output without having received the peer's leaves
 * the input alive. It is what allows detecting a truncation — somebody having cut the connection to
 * make it look as if the message ended there.
 */
public abstract class SSLEngine {

    private final String peerHost;
    private final int peerPort;

    /** Without peer data: a session cannot be resumed nor SNI sent. */
    protected SSLEngine() {
        this(null, -1);
    }

    /**
     * With the suggested peer.
     *
     * <p>It is a <em>hint</em>, not a destination: the engine connects to nothing. It serves two
     * concrete things — resuming a session with that peer, and sending its name through SNI.
     */
    protected SSLEngine(String peerHost, int peerPort) {
        this.peerHost = peerHost;
        this.peerPort = peerPort;
    }

    /** The suggested peer's name, or {@code null}. */
    public String getPeerHost() {
        return this.peerHost;
    }

    /** The suggested peer's port, or {@code -1}. */
    public int getPeerPort() {
        return this.peerPort;
    }

    /** Encrypts the data of {@code src} into {@code dst}. */
    public SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws SSLException {
        return wrap(new ByteBuffer[] { src }, 0, 1, dst);
    }

    /** The same, taking from several buffers. */
    public SSLEngineResult wrap(ByteBuffer[] srcs, ByteBuffer dst) throws SSLException {
        if (srcs == null) {
            throw new IllegalArgumentException("srcs");
        }
        return wrap(srcs, 0, srcs.length, dst);
    }

    /**
     * The general form, over a range of {@code srcs}.
     *
     * <p>It is the only abstract one of the three: the other two delegate here. An implementation
     * writes just one.
     */
    public abstract SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length, ByteBuffer dst)
            throws SSLException;

    /** Decrypts the data of {@code src} into {@code dst}. */
    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws SSLException {
        return unwrap(src, new ByteBuffer[] { dst }, 0, 1);
    }

    /** The same, spreading over several buffers. */
    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts) throws SSLException {
        if (dsts == null) {
            throw new IllegalArgumentException("dsts");
        }
        return unwrap(src, dsts, 0, dsts.length);
    }

    /** The general form, over a range of {@code dsts}. */
    public abstract SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset,
            int length) throws SSLException;

    /**
     * A pending task, or {@code null} if there is none.
     *
     * <p>They are taken and run until it returns {@code null}. They may be run in another thread —
     * that is exactly what they exist for — but <strong>they have to be run</strong>.
     */
    public abstract Runnable getDelegatedTask();

    /**
     * Closes the input.
     *
     * @throws SSLException if the peer had not sent its close alert, which may mean a truncation
     *     and not a close
     */
    public abstract void closeInbound() throws SSLException;

    /** Whether no more input is going to be accepted. */
    public abstract boolean isInboundDone();

    /** Closes the output. A {@link #wrap} still has to be done to emit the alert. */
    public abstract void closeOutbound();

    /** Whether the output close alert was already emitted. */
    public abstract boolean isOutboundDone();

    /** All the suites the engine knows. */
    public abstract String[] getSupportedCipherSuites();

    /** The suites enabled now. */
    public abstract String[] getEnabledCipherSuites();

    /** Sets the enabled suites. */
    public abstract void setEnabledCipherSuites(String[] suites);

    /** All the protocols the engine knows. */
    public abstract String[] getSupportedProtocols();

    /** The protocols enabled now. */
    public abstract String[] getEnabledProtocols();

    /** Sets the enabled protocols. */
    public abstract void setEnabledProtocols(String[] protocols);

    /**
     * The current session.
     *
     * <p>Before the first handshake it returns an empty session, with suite {@code
     * SSL_NULL_WITH_NULL_NULL} — not {@code null}. It is awkward but deliberate: it forces looking
     * at the suite instead of assuming that having a session means being authenticated.
     */
    public abstract SSLSession getSession();

    /**
     * The session being negotiated, or {@code null} if there is no handshake in progress.
     *
     * <p>It exists to be able to decide <em>during</em> the handshake — choosing a certificate by
     * looking at the SNI that just arrived, for example— when {@link #getSession} still returns the
     * old one.
     */
    public SSLSession getHandshakeSession() {
        throw new UnsupportedOperationException("engine exposes no handshake session");
    }

    /** Starts or renegotiates the handshake. */
    public abstract void beginHandshake() throws SSLException;

    /** What has to be done now; see the loop in the class description. */
    public abstract SSLEngineResult.HandshakeStatus getHandshakeStatus();

    /**
     * Whether this engine is the client.
     *
     * @throws IllegalArgumentException if the handshake already started — the role defines the
     *     whole protocol and changing it halfway means nothing
     */
    public abstract void setUseClientMode(boolean mode);

    /** Whether it is the client. */
    public abstract boolean getUseClientMode();

    /** Requires client authentication; only makes sense on the server side. */
    public abstract void setNeedClientAuth(boolean need);

    /** Whether client authentication is required. */
    public abstract boolean getNeedClientAuth();

    /** Requests client authentication without requiring it. */
    public abstract void setWantClientAuth(boolean want);

    /** Whether client authentication is requested. */
    public abstract boolean getWantClientAuth();

    /** Whether new sessions can be created, or only the existing ones resumed. */
    public abstract void setEnableSessionCreation(boolean flag);

    /** Whether new sessions can be created. */
    public abstract boolean getEnableSessionCreation();

    /** All the configuration together; see {@link SSLParameters}. */
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

    /**
     * Applies the configuration.
     *
     * <p>Only what is not {@code null}: a freshly created {@link SSLParameters} has almost
     * everything unset, and applying it whole would erase what the engine already had.
     */
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

    /**
     * The application protocol agreed by ALPN, {@code ""} if none, {@code null} if not negotiated.
     */
    public String getApplicationProtocol() {
        throw new UnsupportedOperationException("this engine does not support ALPN");
    }

    /** The one being agreed while the handshake lasts. */
    public String getHandshakeApplicationProtocol() {
        throw new UnsupportedOperationException("this engine does not support ALPN");
    }

    /**
     * Chooses the application protocol with a function of its own instead of the list.
     *
     * <p>It serves on the server side, where the choice may depend on something only known by
     * looking at the concrete client.
     */
    public void setHandshakeApplicationProtocolSelector(
            BiFunction<SSLEngine, List<String>, String> selector) {
        throw new UnsupportedOperationException("this engine does not support ALPN");
    }

    /** The selector set, or {@code null}. */
    public BiFunction<SSLEngine, List<String>, String> getHandshakeApplicationProtocolSelector() {
        throw new UnsupportedOperationException("this engine does not support ALPN");
    }
}
