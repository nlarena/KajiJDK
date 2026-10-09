package javax.xml.crypto.dsig;

import java.util.List;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;

/**
 * KajiLibrary's javax.xml.crypto.dsig.XMLSignature -- a complete XML signature.
 *
 * <p>The central structure of the package. It has three parts and it is as well to have them clear:
 *
 * <ul>
 *   <li>the {@link SignedInfo} -- <b>the only thing that is signed</b>. It contains the references
 *       to the data and the algorithms used;
 *   <li>the {@link SignatureValue} -- the cryptographic signature of the {@code SignedInfo};
 *   <li>the {@link KeyInfo} -- optional, and see its note: information, not authority.
 * </ul>
 *
 * <h2>The signature covers the SignedInfo, not the data</h2>
 *
 * <p>It is the part of XML-DSig to understand and the one that produces all the misunderstandings:
 * the signature is computed over the {@code SignedInfo}, and the {@code SignedInfo} contains the
 * <b>digest</b> of each datum. The data themselves are not signed directly.
 *
 * <p>Two consequences follow. The good one: anything can be signed, even something external to the
 * document. The dangerous one: {@link #validate} returns true if the {@code SignedInfo}'s signature
 * checks out <b>and</b> all the digests check out, but that says nothing about <b>what</b> was
 * signed. A valid signature over a reference that points to something else is a valid signature.
 *
 * <p>That is why, after validating, two more things have to be looked at: which key it was
 * validated with ({@link #getKeySelectorResult}) and what the references cover.
 */
public interface XMLSignature extends XMLStructure {

    /** The XML-DSig namespace. */
    static final String XMLNS = "http://www.w3.org/2000/09/xmldsig#";

    /**
     * Validates the signature.
     *
     * <p>See the class note: true does not mean that what one believes was signed.
     *
     * @throws XMLSignatureException if the validation could not be done
     */
    boolean validate(XMLValidateContext validateContext) throws XMLSignatureException;

    /** What the signature says about its key, or null. */
    KeyInfo getKeyInfo();

    /** What was really signed. */
    SignedInfo getSignedInfo();

    /** The objects the signature carries inside. Unmodifiable. */
    List<XMLObject> getObjects();

    /** The element's identifier, or null. */
    String getId();

    /** The cryptographic signature. */
    SignatureValue getSignatureValue();

    /**
     * Computes the signature and leaves it in the context.
     *
     * @throws MarshalException if the XML could not be written
     * @throws XMLSignatureException if it could not be signed
     */
    void sign(XMLSignContext signContext) throws MarshalException, XMLSignatureException;

    /**
     * Which key it was validated with.
     *
     * <p>It is what has to be compared against the trust list; see the class note.
     *
     * @return null if it has not been validated yet
     */
    KeySelectorResult getKeySelectorResult();

    /**
     * The value of the signature.
     *
     * <p>It has its own {@link #validate} because a signature can fail in two different ways: the
     * cryptographic value does not check out, or some reference does not check out. Being able to
     * ask them separately is the only way to know which one failed, and that changes the diagnosis
     * -- the first is a wrong key or an altered document; the second, an altered datum.
     */
    public static interface SignatureValue extends XMLStructure {

        /** The element's identifier, or null. */
        String getId();

        /** The bytes of the signature. */
        byte[] getValue();

        /**
         * Whether the cryptographic value checks out, without looking at the references.
         *
         * @throws XMLSignatureException if the validation could not be done
         */
        boolean validate(XMLValidateContext validateContext) throws XMLSignatureException;
    }
}
