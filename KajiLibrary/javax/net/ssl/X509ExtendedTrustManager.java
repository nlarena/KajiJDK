package javax.net.ssl;

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * An {@link X509TrustManager} that also sees <strong>whom</strong> one is talking to.
 *
 * <h2>Why that changes everything</h2>
 *
 * <p>The methods of {@link X509TrustManager} receive the chain and the authentication type, and
 * nothing more. With that one can check that the certificate is valid and signed by somebody
 * trusted — but <strong>not</strong> that it belongs to whom we connected to, because that datum
 * does not arrive.
 *
 * <p>These four receive the {@link Socket} or the {@link SSLEngine}, that is the name that was
 * asked for. It is what allows endpoint identity checking, the one that stops an attacker with a
 * legitimate certificate for another domain. A manager that implements only the old interface
 * leaves that hole open, and that is why the JDK uses this class whenever it can.
 */
public abstract class X509ExtendedTrustManager implements X509TrustManager {

    /** For subclasses. */
    public X509ExtendedTrustManager() {
    }

    /**
     * @throws CertificateException if the client is not trustworthy
     */
    public abstract void checkClientTrusted(X509Certificate[] chain, String authType,
            Socket socket) throws CertificateException;

    /**
     * @throws CertificateException if the server is not trustworthy, identity failure included
     */
    public abstract void checkServerTrusted(X509Certificate[] chain, String authType,
            Socket socket) throws CertificateException;

    /**
     * @throws CertificateException if the client is not trustworthy
     */
    public abstract void checkClientTrusted(X509Certificate[] chain, String authType,
            SSLEngine engine) throws CertificateException;

    /**
     * @throws CertificateException if the server is not trustworthy
     */
    public abstract void checkServerTrusted(X509Certificate[] chain, String authType,
            SSLEngine engine) throws CertificateException;
}
