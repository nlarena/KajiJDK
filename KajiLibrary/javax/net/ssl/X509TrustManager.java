package javax.net.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Decides whether an X.509 certificate chain is trustworthy.
 *
 * <h2>Why the methods do not return a boolean</h2>
 *
 * <p>Because an exception can say <em>why</em> not, and a {@code false} cannot. Rejecting for an
 * expired certificate, an invalid signature or an unknown issuer are three different situations,
 * and the caller --or whoever reads a log-- needs to tell them apart. Returning normally is
 * accepting.
 *
 * <p>The two methods are not the same question with the roles swapped: for the client the list of
 * accepted issuers matters (see {@link #getAcceptedIssuers}), for the server its identity does. The
 * note said the server is validated here against its name; these methods receive only the chain and
 * the authentication type, so the name never reaches them -- checking it is what
 * {@link X509ExtendedTrustManager} adds.
 */
public interface X509TrustManager extends TrustManager {

    /**
     * @throws CertificateException if the client is not trustworthy, with the reason inside
     */
    void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException;

    /**
     * @throws CertificateException if the server is not trustworthy
     */
    void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException;

    /**
     * The issuers this manager accepts.
     *
     * <p>It is not merely informative: it is what the server sends the client to tell it which
     * certificates will do. Without it the client would have to guess which of its own to present.
     */
    X509Certificate[] getAcceptedIssuers();
}
