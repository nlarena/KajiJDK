package javax.net.ssl;

import java.security.Principal;

/**
 * An {@link X509KeyManager} that can also choose for an {@link SSLEngine}.
 *
 * <h2>Why one more class was needed</h2>
 *
 * <p>Because {@link X509KeyManager} receives a {@link java.net.Socket} to decide, and an {@code
 * SSLEngine} has no socket: it is precisely the abstraction that separated the protocol from the
 * transport. Adding methods to the interface would have broken everybody who already implemented
 * it, so the new ones arrived in an abstract class with a body.
 *
 * <p>Those bodies return {@code null}, which means "I have no credential for this". It is the safe
 * answer: choosing nothing is worse than failing, but much better than presenting a credential that
 * does not correspond.
 */
public abstract class X509ExtendedKeyManager implements X509KeyManager {

    /** For subclasses. */
    protected X509ExtendedKeyManager() {
    }

    /** Chooses the client alias for an engine; {@code null} if none serves. */
    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
        return null;
    }

    /** Chooses the server alias for an engine; {@code null} if none serves. */
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return null;
    }
}
