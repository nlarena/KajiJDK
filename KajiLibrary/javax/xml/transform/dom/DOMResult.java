package javax.xml.transform.dom;

import javax.xml.transform.Result;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.transform.dom.DOMResult -- a DOM tree as the destination of a
 * transformation.
 *
 * <p>It can be used in two ways. Without a node, the transformer creates a new document and leaves
 * it in {@link #getNode}. With a node, the output is <b>appended</b> to that node, which is what
 * allows building a document piece by piece with several transformations.
 *
 * <h2>Where it is inserted</h2>
 *
 * <p>{@link #setNextSibling} decides the exact place: without it, the output goes at the end of the
 * children; with it, right before that sibling. It is the only way of inserting in the middle,
 * because the transformer knows nothing of the document except what this object says.
 *
 * <p>From there comes the validation that surprises: the sibling has to be a <b>child of the
 * node</b>, and if it is not, it is rejected. It has to be so -- a sibling living elsewhere in the
 * tree would describe a place that does not exist within the target node--, and the validation
 * happens when building instead of when transforming, which is when it would be too late to fix it.
 *
 * <p>The two routes answer with different exceptions and it is not an oversight: in the constructor
 * it is {@link IllegalArgumentException} because they are arguments inconsistent with each other,
 * and in {@link #setNextSibling} it is {@link IllegalStateException} because the argument
 * contradicts the node the object <b>already had</b>.
 */
public class DOMResult implements Result {

    /** With this a {@code TransformerFactory} is asked whether it accepts this destination. */
    public static final String FEATURE = "http://javax.xml.transform.dom.DOMResult/feature";

    private Node node;

    private Node nextSibling;

    private String systemId;

    /** Empty: the transformer creates the document. */
    public DOMResult() {
        setNode(null);
        setNextSibling(null);
        setSystemId(null);
    }

    /** The output is appended at the end of that node's children. */
    public DOMResult(Node node) {
        setNode(node);
        setNextSibling(null);
        setSystemId(null);
    }

    /** Likewise, saying where the result comes from. */
    public DOMResult(Node node, String systemId) {
        setNode(node);
        setNextSibling(null);
        setSystemId(systemId);
    }

    /**
     * The output is inserted before that sibling.
     *
     * @throws IllegalArgumentException if the sibling is not a child of the node; see the class
     *     note
     */
    public DOMResult(Node node, Node nextSibling) {
        if (nextSibling != null) {
            if (node == null) {
                throw new IllegalArgumentException(
                    "Cannot create a DOMResult when the nextSibling is contained by the "
                        + "\"null\" node.");
            }
            if (nextSibling.getParentNode() != node) {
                throw new IllegalArgumentException(
                    "Cannot create a DOMResult when the nextSibling is not contained by the node.");
            }
        }
        setNode(node);
        this.nextSibling = nextSibling;
        setSystemId(null);
    }

    /**
     * Both things.
     *
     * @throws IllegalArgumentException if the sibling is not a child of the node
     */
    public DOMResult(Node node, Node nextSibling, String systemId) {
        this(node, nextSibling);
        setSystemId(systemId);
    }

    /** The node the output is appended to; null for the transformer to create one. */
    public void setNode(Node node) {
        this.node = node;
    }

    /** Ver {@link #setNode}. */
    public Node getNode() {
        return this.node;
    }

    /**
     * Before which sibling the output is inserted; null to append at the end.
     *
     * @throws IllegalStateException if it is not a child of the node this object already has
     */
    public void setNextSibling(Node nextSibling) {
        if (nextSibling != null) {
            if (this.node == null) {
                throw new IllegalStateException(
                    "Cannot create a DOMResult when the nextSibling is contained by the "
                        + "\"null\" node.");
            }
            if (nextSibling.getParentNode() != this.node) {
                throw new IllegalStateException(
                    "Cannot create a DOMResult when the nextSibling is not contained by the node.");
            }
        }
        this.nextSibling = nextSibling;
    }

    /** Ver {@link #setNextSibling}. */
    public Node getNextSibling() {
        return this.nextSibling;
    }

    /** Where the result comes from; informative, nothing is written there. */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /** Ver {@link #setSystemId}. */
    public String getSystemId() {
        return this.systemId;
    }
}
