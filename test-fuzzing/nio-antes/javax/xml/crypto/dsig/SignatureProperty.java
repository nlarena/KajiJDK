package javax.xml.crypto.dsig;

import java.util.List;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.SignatureProperty -- a datum <b>about</b> the signature.
 *
 * <p>Not about what was signed: about the act of signing. The typical case is the moment it was
 * signed, or with which device.
 *
 * <p>{@link #getTarget} is mandatory and says which signature the property refers to, pointing by
 * URI to the signature element. It is needed because properties live inside an {@code Object}, and
 * an {@code Object} can be in a document with several signatures.
 *
 * <p>For the property to be protected a {@link Reference} has to point to it. An unsigned timestamp
 * can be changed by anyone, which is precisely the opposite of what it is put there for.
 */
public interface SignatureProperty extends XMLStructure {

    /** Which signature it refers to. Mandatory. */
    String getTarget();

    /** The element's identifier, or null. */
    String getId();

    /** The content of the property. Unmodifiable and never empty. */
    List<XMLStructure> getContent();
}
