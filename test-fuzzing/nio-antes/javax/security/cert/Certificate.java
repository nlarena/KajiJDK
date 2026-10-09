package javax.security.cert;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;

/**
 * KajiLibrary's javax.security.cert.Certificate -- a certificate, in the old API.
 *
 * <p>This whole package exists for one reason only: {@code javax.net.ssl.SSLSession} declares
 * {@code getPeerCertificateChain()} returning these, and that signature cannot be changed without
 * breaking everything compiled against it. For anything new there is
 * {@link java.security.cert.Certificate}, which is more complete and the one the rest of the
 * platform uses.
 *
 * <h2>The identity is the bytes</h2>
 *
 * <p>{@link #equals} and {@link #hashCode} look at the encoding, not the fields. It is the right
 * thing for something signed: two certificates with the same issuer, the same subject and the same
 * key but different bytes are <b>different documents</b>, and only one of the two has a signature
 * that checks out. Comparing field by field would say they are equal and that is precisely what an
 * attacker would want.
 *
 * <p>If {@code getEncoded} throws, {@link #equals} returns false instead of propagating: the
 * contract of {@code equals} does not allow throwing, and a certificate that cannot be encoded is
 * equal to nothing.
 *
 * <p>Deprecated <b>and marked for removal</b> since Java 9. The replacement is {@code
 * java.security.cert}, which is not an improved version of this but something else: it supports
 * version 3 of the format, with extensions, which is the only thing that serves to validate a chain
 * today.
 */
@Deprecated(since = "9", forRemoval = true)
public abstract class Certificate {

    /** For the subclasses. */
    public Certificate() {
    }

    /**
     * Equality by encoded bytes. See the class note.
     *
     * @return false if either of the two cannot be encoded
     */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Certificate)) {
            return false;
        }
        try {
            byte[] mine = this.getEncoded();
            byte[] theirs = ((Certificate) other).getEncoded();
            if (mine == null || theirs == null || mine.length != theirs.length) {
                return false;
            }
            int i = 0;
            while (i < mine.length) {
                if (mine[i] != theirs[i]) {
                    return false;
                }
                i = i + 1;
            }
            return true;
        } catch (CertificateException e) {
            return false;
        }
    }

    /**
     * The sum of the encoded bytes, each (unsigned) multiplied by its position; 0 if they cannot be
     * obtained, so as not to throw.
     *
     * <p>The note said "sum of the bytes". It is weighted, and it is not what JDK 25 computes:
     * there it is {@code Arrays.hashCode} of the encoding, so {@code {0xff, 0x80, 5}} hashes to
     * 24867 in the JDK and to 138 here.
     */
    public int hashCode() {
        int result = 0;
        try {
            byte[] encoded = this.getEncoded();
            if (encoded == null) {
                return 0;
            }
            int i = 0;
            while (i < encoded.length) {
                result = result + (encoded[i] & 0xff) * i;
                i = i + 1;
            }
        } catch (CertificateException e) {
            return 0;
        }
        return result;
    }

    /**
     * The encoded form, which is the one that was signed.
     *
     * @throws CertificateEncodingException if it cannot be produced
     */
    public abstract byte[] getEncoded() throws CertificateEncodingException;

    /**
     * Verifies the signature with that key, using the default provider.
     *
     * @throws SignatureException if the signature does not check out
     */
    public abstract void verify(PublicKey key)
        throws CertificateException, NoSuchAlgorithmException, InvalidKeyException,
               NoSuchProviderException, SignatureException;

    /**
     * Likewise, asking a named provider for the algorithm.
     *
     * @param sigProvider the provider's name
     */
    public abstract void verify(PublicKey key, String sigProvider)
        throws CertificateException, NoSuchAlgorithmException, InvalidKeyException,
               NoSuchProviderException, SignatureException;

    /** A readable description. The subclasses owe it. */
    public abstract String toString();

    /** The public key the certificate binds to its subject. */
    public abstract PublicKey getPublicKey();
}
