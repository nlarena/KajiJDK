package javax.imageio.metadata;

import java.util.List;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

/**
 * The attributes of an {@link IIOMetadataNode}, seen as a DOM map.
 *
 * <p>Package-private: it is not API. {@code Node.getAttributes} returns it.
 *
 * <p>It is <b>read-only</b>: {@code setNamedItem} and {@code removeNamedItem} throw
 * {@link DOMException} with {@code NO_MODIFICATION_ALLOWED_ERR}. To change attributes there are
 * the element's own methods.
 *
 * <p>Like {@link IIONodeList}, it is a snapshot taken when asked for. (In the JDK this map wraps
 * the node's live attribute list instead.)
 */
class IIONamedNodeMap implements NamedNodeMap {

    /** The attributes. */
    private final List<Node> nodes;

    IIONamedNodeMap(List<Node> nodes) {
        this.nodes = nodes;
    }

    public int getLength() {
        return this.nodes.size();
    }

    /** The attribute with that name, or null. */
    public Node getNamedItem(String name) {
        int i = 0;
        while (i < this.nodes.size()) {
            if (name.equals(this.nodes.get(i).getNodeName())) {
                return this.nodes.get(i);
            }
            i = i + 1;
        }
        return null;
    }

    /** Number {@code index}, or null. */
    public Node item(int index) {
        if (index < 0 || index >= this.nodes.size()) {
            return null;
        }
        return this.nodes.get(index);
    }

    /** No: it is read-only. See the class note. */
    public Node removeNamedItem(String name) {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR,
                               "This NamedNodeMap is read-only!");
    }

    /** No: it is read-only. */
    public Node setNamedItem(Node arg) {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR,
                               "This NamedNodeMap is read-only!");
    }

    /** Namespaces are not supported; see {@link IIOMetadataNode}. */
    public Node getNamedItemNS(String namespaceURI, String localName) {
        return getNamedItem(localName);
    }

    /** No: it is read-only. */
    public Node setNamedItemNS(Node arg) {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR,
                               "This NamedNodeMap is read-only!");
    }

    /** No: it is read-only. */
    public Node removeNamedItemNS(String namespaceURI, String localName) {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR,
                               "This NamedNodeMap is read-only!");
    }
}
