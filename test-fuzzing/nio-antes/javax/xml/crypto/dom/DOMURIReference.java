package javax.xml.crypto.dom;

import javax.xml.crypto.URIReference;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.crypto.dom.DOMURIReference -- a URI reference that knows where it points
 * from.
 *
 * <p>It adds one method over {@link URIReference}: {@link #getHere}, the node where the reference
 * <b>appears</b>.
 *
 * <p>It sounds redundant and it is not. A reference with URI {@code ""} means "the whole document",
 * and one that starts with {@code #} points inside the same document; to resolve either of the two
 * it is necessary to know in which document the reference is written. The URI alone is not enough.
 *
 * <p>Besides, the XPath transform of the signature standard defines the {@code here()} variable
 * exactly as this node -- hence the method's name.
 */
public interface DOMURIReference extends URIReference {

    /** The node where this reference appears. See the class note. */
    Node getHere();
}
