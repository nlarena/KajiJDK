package javax.imageio.metadata;

import java.util.List;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An already materialized list of nodes.
 *
 * <p>Package-private: it is not API. {@code getElementsByTagName} and friends return it.
 *
 * <p>It is a snapshot, not a live view: unlike what DOM promises for {@code getElementsByTagName},
 * adding a node afterwards does not change it. It is what the JDK does, and for image metadata
 * --which is walked and not edited while walked-- it is enough.
 */
class IIONodeList implements NodeList {

    /** The nodes. */
    private final List<Node> nodes;

    IIONodeList(List<Node> nodes) {
        this.nodes = nodes;
    }

    public int getLength() {
        return this.nodes.size();
    }

    /** Node number {@code index}, or null if it does not exist. Never throws. */
    public Node item(int index) {
        if (index < 0 || index >= this.nodes.size()) {
            return null;
        }
        return this.nodes.get(index);
    }
}
