package javax.security.auth.x500;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.security.auth.Destroyable;

/**
 * KajiLibrary's javax.security.auth.x500.X500PrivateCredential -- a complete identity: the
 * certificate, the private key that goes with it, and which entry of the store they came from.
 *
 * <p>It is a pair, not two loose objects, and for a concrete reason: a certificate without its key
 * serves to verify, and a key without its certificate serves for nothing -- nobody knows whom it
 * belongs to. What is needed to <b>act</b> as someone is both together, and this class is that.
 *
 * <p>The arguments of the constructors --certificate, key and alias-- are all mandatory and are
 * rejected with {@code IllegalArgumentException}, not with {@code NullPointerException}. It is what
 * the JDK does and it is worth noting because it is not the usual in the rest of the library. (The
 * note said "the four arguments"; there are three.)
 *
 * <h2>About destroy()</h2>
 *
 * <p>{@code destroy()} <b>does not erase the key</b>: it sets the three references to null and lets
 * go of the object. The private key itself stays in memory until the collector picks it up, and if
 * it is a shared object it stays alive in whoever holds it. It is the same as the JDK does and it
 * has to be said, because the name promises more than the method can give: whoever really wants to
 * erase the material has to call the key's {@code destroy()}, if the key implements it.
 *
 * <p>A detail that surprises: with the two-argument constructor the alias already starts as null,
 * but {@code isDestroyed()} gives false all the same, because it requires the <b>three</b> fields
 * to be.
 */
public final class X500PrivateCredential implements Destroyable {

    private X509Certificate cert;
    private PrivateKey key;
    private String alias;

    /**
     * @throws IllegalArgumentException if any is null
     */
    public X500PrivateCredential(X509Certificate cert, PrivateKey key) {
        if (cert == null || key == null) {
            throw new IllegalArgumentException();
        }
        this.cert = cert;
        this.key = key;
        this.alias = null;
    }

    /**
     * Likewise, also remembering which entry of the store the pair came from.
     *
     * @throws IllegalArgumentException if any is null, the alias included
     */
    public X500PrivateCredential(X509Certificate cert, PrivateKey key, String alias) {
        if (cert == null || key == null || alias == null) {
            throw new IllegalArgumentException();
        }
        this.cert = cert;
        this.key = key;
        this.alias = alias;
    }

    /** The certificate, or null if {@link #destroy} was already called. */
    public X509Certificate getCertificate() {
        return this.cert;
    }

    /** The private key, or null if {@link #destroy} was already called. */
    public PrivateKey getPrivateKey() {
        return this.key;
    }

    /** The alias in the store, or null if none was given or {@link #destroy} was already called. */
    public String getAlias() {
        return this.alias;
    }

    /**
     * Lets go of the three references. See the class note on what this does not do.
     *
     * <p>It does not declare {@code DestroyFailedException}: letting go of a reference cannot fail.
     * Calling it twice cannot either.
     */
    public void destroy() {
        this.cert = null;
        this.key = null;
        this.alias = null;
    }

    /** Whether the three references have been let go. */
    public boolean isDestroyed() {
        return this.cert == null && this.key == null && this.alias == null;
    }
}
