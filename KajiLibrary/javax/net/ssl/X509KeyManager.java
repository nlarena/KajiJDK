package javax.net.ssl;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Chooses which own certificate to present, among the ones there are.
 *
 * <h2>The aliases, and why there are two levels</h2>
 *
 * <p>{@link #getClientAliases} and {@link #getServerAliases} <em>enumerate</em> the candidates;
 * {@link #chooseClientAlias} and {@link #chooseServerAlias} <em>choose</em> one. They are separate
 * because enumerating is a query without context and choosing depends on whom one is talking to —
 * hence the latter receive the {@link Socket}.
 *
 * <p>The {@code issuers} parameter is the list of issuers the peer declared it accepts. Presenting
 * a certificate signed by somebody not on that list is a guarantee of rejection.
 */
public interface X509KeyManager extends KeyManager {

    /** The usable client aliases for that key type and those issuers. */
    String[] getClientAliases(String keyType, Principal[] issuers);

    /** Chooses the client alias, or {@code null} if none serves. */
    String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket);

    /** The usable server aliases. */
    String[] getServerAliases(String keyType, Principal[] issuers);

    /** Chooses the server alias, or {@code null}. */
    String chooseServerAlias(String keyType, Principal[] issuers, Socket socket);

    /** That alias's certificate chain. */
    X509Certificate[] getCertificateChain(String alias);

    /** That alias's private key. */
    PrivateKey getPrivateKey(String alias);
}
