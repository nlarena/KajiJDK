package javax.net.ssl;

/**
 * The last word on whether the server's name corresponds to its certificate.
 *
 * <h2>Why this exists and is an extension point</h2>
 *
 * <p>That a certificate is valid and signed by somebody trusted <strong>does not say it belongs to
 * whom we connected to</strong>: a legitimate certificate of another site passes every
 * cryptographic check. Comparing the requested name against the certificate's is a separate step,
 * and it is the one that stops an attacker who got a valid certificate for any other domain.
 *
 * <p>It is consulted <em>only when the standard check already failed</em>. Returning {@code true}
 * from here cancels that protection, which is why it is so easy to turn off TLS security without
 * noticing.
 */
public interface HostnameVerifier {

    /**
     * @return {@code true} to accept the connection even though the name did not match
     */
    boolean verify(String hostname, SSLSession session);
}
