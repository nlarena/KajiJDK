package javax.net.ssl;

import java.util.Enumeration;

/**
 * The set of sessions that can be resumed, with its expiry policy.
 *
 * <p>Resuming saves a complete handshake, but keeping sessions forever has two costs: memory, and
 * --the one that matters-- <strong>security</strong>. An old session still has a usable master
 * secret, so the longer it lives, the more it is worth stealing. The two limits of this interface
 * are that compromise: {@link #setSessionTimeout} by time and {@link #setSessionCacheSize} by
 * count.
 *
 * <p>There is one on the client side and another on the server side, and {@link SSLContext} gives
 * them separately: the two roles keep different things and have different reasons to forget them.
 */
public interface SSLSessionContext {

    /** The session with that identifier, or {@code null} if it is not there or expired. */
    SSLSession getSession(byte[] sessionId);

    /** The identifiers of the current sessions. */
    Enumeration<byte[]> getIds();

    /**
     * How many seconds a session lives unused; {@code 0} is unlimited.
     *
     * @throws IllegalArgumentException if it is negative
     */
    void setSessionTimeout(int seconds);

    /** The current time limit. */
    int getSessionTimeout();

    /**
     * How many sessions to keep; {@code 0} is unlimited.
     *
     * @throws IllegalArgumentException if it is negative
     */
    void setSessionCacheSize(int size);

    /** The current count limit. */
    int getSessionCacheSize();
}
