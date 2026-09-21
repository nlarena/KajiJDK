package org.w3c.dom.traversal;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.traversal.TreeWalker -- it walks a document <b>as a tree</b>.
 *
 * <p>It is the other half of {@code org.w3c.dom.traversal}. Where {@link NodeIterator} flattens the
 * document to a list, this one keeps the shape: it has {@link #parentNode()}, {@link #firstChild()}
 * and siblings, and presents a <b>pruned</b> tree -- the one left after applying the mask and the
 * filter.
 *
 * <h2>The tree one sees is not the one there is</h2>
 *
 * <p>And there lies what is surprising: if an intermediate node gives {@code FILTER_SKIP}, its
 * children are <b>promoted</b>, so {@link #parentNode()} from one of them returns the
 * <b>grandparent</b>. The walk is consistent with itself, but it does not coincide with the
 * {@code Node.getParentNode()} of the document. Whoever mixes the two gets lost.
 *
 * <p>It is also where {@code FILTER_REJECT} is told apart from {@code FILTER_SKIP}: here rejecting
 * prunes the whole subtree, while in an iterator the two do the same.
 *
 * <h2>The current node may be outside the view</h2>
 *
 * <p>{@link #setCurrentNode} accepts <b>any</b> node, even one the filter hides and even one
 * outside the root. It is not an oversight of the specification: it serves for repositioning the
 * walk from a node obtained by another road. The later movements do respect the filter, so from a
 * hidden node one gets out at the first move.
 */
public interface TreeWalker {

    /** The root of the walk. No movement leaves its subtree. */
    Node getRoot();

    /** The mask of types, an OR of the {@code SHOW_*} of {@link NodeFilter}. */
    int getWhatToShow();

    /** The filter, or null if there is none. */
    NodeFilter getFilter();

    /** Whether entity references are expanded while walking. */
    boolean getExpandEntityReferences();

    /** Where it is standing. It may be a node the filter hides; see the note of the class. */
    Node getCurrentNode();

    /**
     * It repositions it.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR} if it is null
     */
    void setCurrentNode(Node currentNode) throws DOMException;

    /**
     * It goes up to the <b>visible</b> parent, or null if there is none inside the root.
     *
     * <p>It may not be the real parent; see the note of the class.
     */
    Node parentNode();

    /** The first visible child, or null. */
    Node firstChild();

    /** The last visible child, or null. */
    Node lastChild();

    /** The previous visible sibling, or null. */
    Node previousSibling();

    /** The next visible sibling, or null. */
    Node nextSibling();

    /** The previous one in document order inside the pruned tree, or null. */
    Node previousNode();

    /** The next one in document order inside the pruned tree, or null. */
    Node nextNode();
}
