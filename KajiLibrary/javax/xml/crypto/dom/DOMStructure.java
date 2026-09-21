package javax.xml.crypto.dom;

import javax.xml.crypto.XMLStructure;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.crypto.dom.DOMStructure -- a DOM node seen as an XML signature structure.
 *
 * <p>The adapter between the two worlds. The signature APIs speak {@link XMLStructure}; the
 * concrete content of a signature --the {@code KeyInfo} of a custom format, an {@code Object} with
 * arbitrary data-- arrives as DOM. This class is the bridge, and it is one line.
 *
 * <p>It is immutable and does not copy the node: it keeps the reference. Modifying the tree after
 * wrapping it changes what is signed.
 *
 * <p>{@link #isFeatureSupported} returns false for everything, the {@code "DOM"} mechanism
 * included. It is what the JDK does: the class supports no declarable feature.
 */
public class DOMStructure implements XMLStructure {

    /** The wrapped node. */
    private final Node node;

    /**
     * @param node the node; it is not copied
     * @throws NullPointerException if it is null
     */
    public DOMStructure(Node node) {
        if (node == null) {
            throw new NullPointerException("node cannot be null");
        }
        this.node = node;
    }

    /** The wrapped node. */
    public Node getNode() {
        return this.node;
    }

    /**
     * Always false. See the class note.
     *
     * @throws NullPointerException if the name is null
     */
    public boolean isFeatureSupported(String feature) {
        if (feature == null) {
            throw new NullPointerException();
        }
        return false;
    }
}
