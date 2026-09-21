package javax.imageio.metadata;

import javax.imageio.IIOException;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.imageio.metadata.IIOInvalidTreeException -- that metadata tree is not valid.
 *
 * <p>Thrown by {@code IIOMetadata.setFromTree} and {@code mergeTree} when the tree they are given
 * does not follow the declared format.
 *
 * <p>What makes it useful is {@link #getOffendingNode}: it says <b>which</b> node is wrong. A
 * metadata tree has dozens of nodes, and a message like "invalid attribute" without saying where
 * forces you to search for it by hand.
 *
 * <p>It may return null if the problem is with the whole tree --the root is not the one the format
 * asks for-- and not with a particular node.
 */
public class IIOInvalidTreeException extends IIOException {

    private static final long serialVersionUID = -1314083172544132777L;

    /** Which node is wrong, or null. */
    protected Node offendingNode = null;

    /**
     * @param message what is wrong
     * @param offendingNode which node, or null
     */
    public IIOInvalidTreeException(String message, Node offendingNode) {
        super(message);
        this.offendingNode = offendingNode;
    }

    /**
     * Same, wrapping the original.
     *
     * @param cause what failed while the tree was being walked
     */
    public IIOInvalidTreeException(String message, Throwable cause, Node offendingNode) {
        super(message, cause);
        this.offendingNode = offendingNode;
    }

    /** Which node is wrong, or null. See the class note. */
    public Node getOffendingNode() {
        return this.offendingNode;
    }
}
