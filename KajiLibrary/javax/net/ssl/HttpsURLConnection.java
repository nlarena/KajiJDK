package javax.net.ssl;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Optional;

/**
 * An {@link HttpURLConnection} over TLS, with what is needed to inspect the channel.
 *
 * <h2>What it adds, and why</h2>
 *
 * <p>{@code HttpURLConnection} delivers the content and says nothing about how it travelled. With
 * HTTPS that is not enough: some decisions depend on whom one really talked to --which certificate
 * it presented, which suite was agreed-- and without these methods one would have to trust blindly
 * that the library did the check right.
 *
 * <h2>The two levels of configuration</h2>
 *
 * <p>Each option comes twice: a static one and an instance one. The static one changes the default
 * of <em>every future connection</em>; the instance one only this one. Both are there because the
 * common case --trusting an own CA for the whole program-- should not force touching every
 * connection, and the rare case --a single different connection-- should not force changing
 * everybody's policy.
 *
 * <p>Careful with the static one: it is global state, and a permissive {@link HostnameVerifier} put
 * there turns off identity checking in the whole program, including code not written by whoever
 * put it there.
 */
public abstract class HttpsURLConnection extends HttpURLConnection {

    /**
     * The default verifier: it always rejects.
     *
     * <p>And that is right, not a limitation: this verifier is only consulted when the standard
     * identity check <strong>already failed</strong>, so returning {@code true} would be accepting
     * a certificate that does not belong to the destination.
     *
     * <p>It is a named class and not an anonymous one --which is how the JDK writes it-- because of
     * finding #499: the frozen javac that builds this library does not support an anonymous class
     * in a field initializer. Inside a method it does, and a named one too; the combination of the
     * two is what fails. (#499 is closed in the source-built javac; checked 2026-09-18.)
     */
    private static final class RejectsEverything implements HostnameVerifier {

        public boolean verify(String hostname, SSLSession session) {
            return false;
        }
    }

    private static HostnameVerifier defaultHostnameVerifier = new RejectsEverything();

    private static SSLSocketFactory defaultSSLSocketFactory;

    /** This connection's verifier. */
    protected HostnameVerifier hostnameVerifier = defaultHostnameVerifier;

    private SSLSocketFactory sslSocketFactory = getDefaultSSLSocketFactory();

    /** For subclasses. */
    protected HttpsURLConnection(URL url) {
        super(url);
    }

    /**
     * The agreed suite.
     *
     * @throws IllegalStateException if the connection is not established yet — there is no suite
     *     before the handshake, and returning {@code null} would let the badly asked question
     *     through
     */
    public abstract String getCipherSuite();

    /** The certificates that were presented, or {@code null}. */
    public abstract Certificate[] getLocalCertificates();

    /**
     * The server's certificates.
     *
     * @throws SSLPeerUnverifiedException if it did not authenticate
     */
    public abstract Certificate[] getServerCertificates() throws SSLPeerUnverifiedException;

    /**
     * Who the server is.
     *
     * <p>By default it comes from the first certificate of {@link #getServerCertificates}, which is
     * what corresponds with X.509. A subclass that uses another authentication overrides it.
     *
     * @throws SSLPeerUnverifiedException if it did not authenticate
     */
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        Certificate[] certs = getServerCertificates();
        if (certs.length == 0 || !(certs[0] instanceof java.security.cert.X509Certificate)) {
            throw new SSLPeerUnverifiedException("there is no X.509 peer certificate");
        }
        return ((java.security.cert.X509Certificate) certs[0]).getSubjectX500Principal();
    }

    /** Who we presented ourselves as, or {@code null}. */
    public Principal getLocalPrincipal() {
        Certificate[] certs = getLocalCertificates();
        if (certs == null || certs.length == 0
                || !(certs[0] instanceof java.security.cert.X509Certificate)) {
            return null;
        }
        return ((java.security.cert.X509Certificate) certs[0]).getSubjectX500Principal();
    }

    /**
     * Changes the default verifier of all future connections.
     *
     * @throws IllegalArgumentException if it is {@code null}
     */
    public static void setDefaultHostnameVerifier(HostnameVerifier v) {
        if (v == null) {
            throw new IllegalArgumentException("the verifier cannot be null");
        }
        defaultHostnameVerifier = v;
    }

    /** The default verifier. */
    public static HostnameVerifier getDefaultHostnameVerifier() {
        return defaultHostnameVerifier;
    }

    /**
     * Changes this connection's verifier.
     *
     * @throws IllegalArgumentException if it is {@code null}
     */
    public void setHostnameVerifier(HostnameVerifier v) {
        if (v == null) {
            throw new IllegalArgumentException("the verifier cannot be null");
        }
        this.hostnameVerifier = v;
    }

    /** This connection's verifier. */
    public HostnameVerifier getHostnameVerifier() {
        return this.hostnameVerifier;
    }

    /**
     * Changes the default socket factory.
     *
     * @throws IllegalArgumentException if it is {@code null}
     */
    public static void setDefaultSSLSocketFactory(SSLSocketFactory sf) {
        if (sf == null) {
            throw new IllegalArgumentException("the factory cannot be null");
        }
        defaultSSLSocketFactory = sf;
    }

    /** The default factory; {@link SSLSocketFactory#getDefault}'s if it was not changed. */
    public static synchronized SSLSocketFactory getDefaultSSLSocketFactory() {
        if (defaultSSLSocketFactory == null) {
            defaultSSLSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
        return defaultSSLSocketFactory;
    }

    /**
     * Changes this connection's factory. It is how an own {@link SSLContext} is used for a single
     * one.
     *
     * @throws IllegalArgumentException if it is {@code null}
     */
    public void setSSLSocketFactory(SSLSocketFactory sf) {
        if (sf == null) {
            throw new IllegalArgumentException("the factory cannot be null");
        }
        this.sslSocketFactory = sf;
    }

    /** This connection's factory. */
    public SSLSocketFactory getSSLSocketFactory() {
        return this.sslSocketFactory;
    }

    /**
     * The session, if there is one.
     *
     * <p>An {@link Optional} and not {@code null} because it came later, and because the legitimate
     * answer is "not yet" — it arrives before the connection is established.
     */
    public Optional<SSLSession> getSSLSession() {
        return Optional.empty();
    }
}
