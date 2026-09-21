package javax.xml.crypto.dsig.keyinfo;

import java.util.List;
import javax.xml.crypto.Data;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.Transform;

/**
 * KajiLibrary's javax.xml.crypto.dsig.keyinfo.RetrievalMethod -- where to fetch the key from.
 *
 * <p>Instead of bringing the key, it brings a URI to get it from, with optional transforms to
 * extract it from whatever is found there.
 *
 * <p>It is the element of {@link KeyInfo} that calls for most care, because {@link #dereference}
 * <b>goes and fetches</b> something whoever signed chose. On a signature of unknown origin that is
 * a network request or a file read the program did not ask for -- the same problem {@code
 * URIDereferencer} exists to control.
 *
 * <p>Its legitimate use is inside one document: a signature that points to another's {@code
 * KeyInfo} so as not to repeat the certificate.
 */
public interface RetrievalMethod extends URIReference, XMLStructure {

    /** The transforms to apply to whatever is found. Unmodifiable. */
    List<Transform> getTransforms();

    /** Where it points. */
    String getURI();

    /**
     * Goes and fetches it. See the class note.
     *
     * @throws URIReferenceException if it cannot be resolved
     */
    Data dereference(XMLCryptoContext context) throws URIReferenceException;
}
