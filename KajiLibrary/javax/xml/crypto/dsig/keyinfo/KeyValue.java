package javax.xml.crypto.dsig.keyinfo;

import java.security.KeyException;
import java.security.PublicKey;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.keyinfo.KeyValue -- the public key, in the clear.
 *
 * <p>The numbers of the public key written into the document: the modulus and exponent of an RSA
 * one, the parameters of a DSA one, the point of an EC one.
 *
 * <p>It is the <b>opposite</b> of {@link KeyName} as far as trust goes. Validating a signature with
 * the key the signature itself brings proves nothing: whoever forges it puts in their own. Using it
 * that way is the commonest mistake of XML-DSig, and it is easy to make because it makes everything
 * "work".
 *
 * <p>It serves two legitimate purposes: comparing the document's key against one known, and
 * carrying a key over a channel where trust was already established some other way.
 *
 * <p>The three type URIs name the three families the standard defines.
 */
public interface KeyValue extends XMLStructure {

    /** A DSA key. */
    static final String DSA_TYPE = "http://www.w3.org/2000/09/xmldsig#DSAKeyValue";

    /** An RSA key. */
    static final String RSA_TYPE = "http://www.w3.org/2000/09/xmldsig#RSAKeyValue";

    /** An elliptic curve key. */
    static final String EC_TYPE = "http://www.w3.org/2009/xmldsig11#ECKeyValue";

    /**
     * The public key.
     *
     * @throws KeyException if the document's numbers do not make a key, or if its algorithm is not
     *     supported
     */
    PublicKey getPublicKey() throws KeyException;
}
