package javax.naming.ldap;

import java.io.IOException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

/**
 * The response to a {@link StartTlsRequest}, which also <strong>does</strong> the negotiation.
 *
 * <h2>Why a response has methods that act</h2>
 *
 * <p>It is this class's anomaly, and it has a reason: the server answers that it agrees to switch
 * to TLS, and the handshake has to happen <em>over the same connection</em>. The only object that
 * has that connection at hand is the one the provider built for the response.
 *
 * <p>Hence the mandatory order: configure --{@link #setEnabledCipherSuites},
 * {@link #setHostnameVerifier}-- and only then {@link #negotiate}. Configuring afterwards does
 * nothing.
 *
 * <h2>The hostname verifier, which is the delicate point</h2>
 *
 * <p>After the handshake you have to check that the certificate belongs to the server you believed
 * you were connected to. The default implementation does it; setting a permissive
 * {@link HostnameVerifier} turns it off, and with that the protection against a man in the middle
 * is lost -- which is exactly what StartTLS came to solve.
 */
public abstract class StartTlsResponse implements ExtendedResponse {

    private static final long serialVersionUID = 8372842182579276418L;

    /** The operation's OID. */
    public static final String OID = "1.3.6.1.4.1.1466.20037";

    /** For the provider's implementations. */
    protected StartTlsResponse() {
    }

    public String getID() {
        return OID;
    }

    /** {@code null}: this response carries no data. */
    public byte[] getEncodedValue() {
        return null;
    }

    /**
     * Restricts the suites to use. It has to be called <strong>before</strong> {@link #negotiate}.
     */
    public abstract void setEnabledCipherSuites(String[] suites);

    /** Changes how the hostname is verified; see the class note before using it. */
    public abstract void setHostnameVerifier(HostnameVerifier verifier);

    /**
     * Does the handshake with the default socket factory.
     *
     * @throws IOException if the handshake fails, or if the hostname does not verify
     */
    public abstract SSLSession negotiate() throws IOException;

    /** Same, with that factory -- that is how you use a TLS context of your own. */
    public abstract SSLSession negotiate(SSLSocketFactory factory) throws IOException;

    /**
     * Closes the TLS layer and goes back to the cleartext connection.
     *
     * <p>The LDAP connection <strong>stays</strong>: this does not close it. It is what allows
     * dropping encryption after a sensitive operation without reconnecting -- though in practice
     * almost nobody wants that.
     */
    public abstract void close() throws IOException;
}
