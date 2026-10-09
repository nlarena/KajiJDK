package javax.xml.crypto.dsig;

import java.util.List;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.XMLObject -- content that travels inside the signature.
 *
 * <p>A free container: it can carry any XML. It is XML-DSig's extension mechanism, and it is used
 * above all for two things: carrying the signed data inside the signature itself --the
 * <b>enveloping</b> signature-- and carrying properties about the signature, like the moment it was
 * made.
 *
 * <p>It is worth insisting on something: being inside the signature does <b>not</b> mean being
 * signed. An {@code Object} is only covered if some {@link Reference} points to it. It is the most
 * frequent confusion of the package, and it produces signatures where the interesting datum is not
 * protected.
 */
public interface XMLObject extends XMLStructure {

    /** The type URI of this element. */
    static final String TYPE = "http://www.w3.org/2000/09/xmldsig#Object";

    /** What it carries inside. Unmodifiable. */
    List<XMLStructure> getContent();

    /** The identifier; it is what a {@link Reference} points to. */
    String getId();

    /** The content type, or null. */
    String getMimeType();

    /** How it is encoded, or null. */
    String getEncoding();
}
