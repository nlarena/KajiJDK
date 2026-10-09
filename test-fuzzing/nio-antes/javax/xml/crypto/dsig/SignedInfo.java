package javax.xml.crypto.dsig;

import java.io.InputStream;
import java.util.List;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.SignedInfo -- the only thing the signature covers.
 *
 * <p>See the note of {@link XMLSignature}: the cryptographic signature is computed over this, and
 * this contains the digests of the data. Everything inside it is protected; everything that is not,
 * is not.
 *
 * <p>It carries both algorithms --how to canonicalize and how to sign-- inside what is signed, and
 * that is deliberate: if the algorithm were outside, an attacker could swap it for a weak one
 * without breaking the signature.
 *
 * <p>{@link #getCanonicalizedData} returns exactly the bytes that were signed. It is the only way
 * to debug a signature that does not validate: comparing those bytes on both sides shows where the
 * canonicalization differs, which is the commonest cause.
 */
public interface SignedInfo extends XMLStructure {

    /** How what is signed is turned into bytes. */
    CanonicalizationMethod getCanonicalizationMethod();

    /** What algorithm it is signed with. */
    SignatureMethod getSignatureMethod();

    /** The covered data, one per reference. Unmodifiable and never empty. */
    List<Reference> getReferences();

    /** The element's identifier, or null. */
    String getId();

    /**
     * The bytes that were really signed.
     *
     * @return null if they have not been computed yet
     */
    InputStream getCanonicalizedData();
}
