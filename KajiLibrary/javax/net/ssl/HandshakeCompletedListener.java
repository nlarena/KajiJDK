package javax.net.ssl;

import java.util.EventListener;

/**
 * Finds out when a handshake finished over an {@link SSLSocket}.
 *
 * <p>It is useful because the handshake is not only the start: TLS allows <em>renegotiating</em>
 * over a connection in progress, and then the session changes — another cipher suite, another peer
 * certificate. A program that decided something by looking at the session has to be able to find
 * out that the decision went stale.
 */
public interface HandshakeCompletedListener extends EventListener {

    /** The handshake finished; the event brings the resulting session. */
    void handshakeCompleted(HandshakeCompletedEvent event);
}
