package javax.xml.transform.dom;

import javax.xml.transform.Source;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.transform.dom.DOMSource -- a DOM tree as the source of a transformation.
 *
 * <p>It carries a node, not necessarily a document. That is the part that matters: <b>any
 * subtree</b> can be transformed by passing the element that heads it, without copying it or taking
 * it out of the document where it lives.
 *
 * <p>The system identifier goes separately from the node because a tree in memory does not know
 * where it came from. It is used to resolve relative references --a {@code document()} inside the
 * stylesheet, for example-- and that is why it is worth setting even if the node is already built.
 */
public class DOMSource implements Source {

    /** With this a {@code TransformerFactory} is asked whether it accepts this source. */
    public static final String FEATURE = "http://javax.xml.transform.dom.DOMSource/feature";

    private Node node;

    private String systemId;

    /** Empty, to be filled with {@link #setNode}. */
    public DOMSource() {
    }

    /**
     * With a node.
     *
     * @param n any node, not only a document; see the class note
     */
    public DOMSource(Node n) {
        setNode(n);
    }

    /**
     * With a node and where it came from.
     *
     * @param systemId what relative things are resolved against
     */
    public DOMSource(Node node, String systemId) {
        setNode(node);
        setSystemId(systemId);
    }

    /** The node to transform. */
    public void setNode(Node node) {
        this.node = node;
    }

    /** Ver {@link #setNode}. */
    public Node getNode() {
        return this.node;
    }

    /** Where the tree came from. See the class note. */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /** Ver {@link #setSystemId}. */
    public String getSystemId() {
        return this.systemId;
    }

    /**
     * Whether this source says nothing.
     *
     * <p>It looks at the node <b>and</b> the identifier, and it needs explaining because the result
     * surprises: a source with an identifier and without a node counts as not empty, even though
     * this class does not know how to fetch anything from that identifier.
     *
     * <p>It makes sense all the same. The question is not "do I have a tree" but "was I given
     * something": a source with an identifier is one somebody filled in, and whoever receives it
     * can resolve it on their own. Answering that it is empty would make the only thing put into it
     * be discarded silently.
     */
    public boolean isEmpty() {
        return getNode() == null && getSystemId() == null;
    }
}
