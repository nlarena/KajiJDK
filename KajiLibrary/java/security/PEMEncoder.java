package java.security;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.EncryptedPrivateKeyInfo;

/**
 * Writes keys and certificates in PEM: base64 between two lines of dashes.
 *
 * <h2>What a text format is for</h2>
 *
 * <p>Because binary does not survive the trip. A key in DER put through an email, a form or a
 * configuration file gets corrupted; in base64 with two lines delimiting it, it does not. The whole
 * format is that: a label saying what is inside and the content in base64 cut into lines of
 * sixty-four characters.
 *
 * <h2>It is immutable</h2>
 *
 * <p>{@link #withEncryption} does not change this encoder: it returns another one. That is what
 * allows a single shared one -- {@link #of}'s -- without anybody being able to reconfigure it from
 * underneath.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>Encoding works for everything that knows how to give its bytes: {@link PEMRecord},
 * {@link EncryptedPrivateKeyInfo}, keys and certificates. What does not work is
 * {@link #withEncryption}, which needs password-based encryption, and no registered provider offers
 * ciphers -- see {@code javax.crypto.Cipher}'s note.
 *
 * @since 25
 */
public final class PEMEncoder {

    private static final PEMEncoder ONLY = new PEMEncoder(null);

    private final char[] password;

    private PEMEncoder(char[] password) {
        this.password = password;
    }

    /**
     * The usual encoder.
     *
     * <p>It always returns the same object: it has no state worth duplicating.
     *
     * @return the encoder
     */
    public static PEMEncoder of() {
        return ONLY;
    }

    /**
     * That in PEM.
     *
     * @param de what is being encoded
     * @return the text, ending in a line break
     * @throws NullPointerException if it is {@code null}
     * @throws IllegalArgumentException if it cannot be encoded
     */
    public String encodeToString(DEREncodable de) {
        if (de == null) {
            throw new NullPointerException("de");
        }
        if (de instanceof PEMRecord) {
            return de.toString();
        }
        if (de instanceof KeyPair) {
            final KeyPair pair = (KeyPair) de;
            return encodeToString(pair.getPublic()) + encodeToString(pair.getPrivate());
        }
        if (de instanceof X509Certificate) {
            return block("CERTIFICATE", encoded((X509Certificate) de));
        }
        if (de instanceof X509CRL) {
            return block("X509 CRL", encoded((X509CRL) de));
        }
        if (de instanceof EncryptedPrivateKeyInfo) {
            return block("ENCRYPTED PRIVATE KEY", encoded((EncryptedPrivateKeyInfo) de));
        }
        if (de instanceof X509EncodedKeySpec) {
            return block("PUBLIC KEY", ((X509EncodedKeySpec) de).getEncoded());
        }
        if (de instanceof PKCS8EncodedKeySpec) {
            return block("PRIVATE KEY", ((PKCS8EncodedKeySpec) de).getEncoded());
        }
        if (de instanceof PublicKey) {
            return block("PUBLIC KEY", encoded((Key) de));
        }
        if (de instanceof PrivateKey) {
            if (this.password != null) {
                throw new IllegalArgumentException(
                        "there is no password-based encryption: no registered provider offers the"
                                + " Cipher service");
            }
            return block("PRIVATE KEY", encoded((Key) de));
        }
        throw new IllegalArgumentException("do not know how to encode " + de.getClass().getName());
    }

    /**
     * The same, in bytes.
     *
     * @param de what is being encoded
     * @return the text in UTF-8
     * @throws NullPointerException if it is {@code null}
     * @throws IllegalArgumentException if it cannot be encoded
     */
    public byte[] encode(DEREncodable de) {
        return encodeToString(de).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Another encoder that encrypts private keys with that password.
     *
     * <p>This one does not change: see the class note.
     *
     * @param password the password
     * @return the other encoder
     * @throws NullPointerException if the password is {@code null}
     */
    public PEMEncoder withEncryption(char[] password) {
        if (password == null) {
            throw new NullPointerException("password");
        }
        return new PEMEncoder(password.clone());
    }

    /** The assembled block, with the content in base64. */
    static String block(String type, byte[] content) {
        return new PEMRecord(type, Base64.getEncoder().encodeToString(content)).toString();
    }

    private static byte[] encoded(Key k) {
        final byte[] b = k.getEncoded();
        if (b == null) {
            throw new IllegalArgumentException("the key does not let itself be encoded");
        }
        return b;
    }

    private static byte[] encoded(X509Certificate c) {
        try {
            return c.getEncoded();
        } catch (java.security.cert.CertificateEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static byte[] encoded(X509CRL c) {
        try {
            return c.getEncoded();
        } catch (java.security.cert.CRLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static byte[] encoded(EncryptedPrivateKeyInfo e) {
        try {
            return e.getEncoded();
        } catch (java.io.IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
