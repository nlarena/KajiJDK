package org.w3c.dom.traversal;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.traversal.NodeIterator -- it walks a document as if it were a list.
 *
 * <p>It flattens the tree to its document order and goes back and forth over it. It is the simple
 * half of {@code org.w3c.dom.traversal}; the other, {@link TreeWalker}, keeps the shape of the
 * tree.
 *
 * <h2>The position is between two nodes, not on one</h2>
 *
 * <p>It is what makes {@link #nextNode()} and {@link #previousNode()} behave as one expects when
 * changing direction: the position is a <b>gap</b> in the list, so calling `nextNode` and then
 * `previousNode` returns <b>the same node</b>, not the previous one. Whoever reads it as a cursor
 * on a node loses one each time they turn round.
 *
 * <h2>The iterator stays alive if the document changes</h2>
 *
 * <p>It does not throw {@code ConcurrentModificationException}: it <b>adjusts</b>. If somebody
 * deletes the node where it was standing, the iterator rearranges itself so that the walk still
 * makes sense. That makes it useful and expensive at the same time, and that is why {@link
 * #detach()} exists: until it is called, the document has to keep telling it about every change.
 */
public interface NodeIterator {

    /** The root of the walk. */
    Node getRoot();

    /** The mask of types, an OR of the {@code SHOW_*} of {@link NodeFilter}. */
    int getWhatToShow();

    /** The filter, or null if there is none. */
    NodeFilter getFilter();

    /** Whether entity references are expanded while walking. */
    boolean getExpandEntityReferences();

    /**
     * The next visible node, or null if it ran out.
     *
     * @throws DOMException {@code INVALID_STATE_ERR} if {@link #detach()} was already called
     */
    Node nextNode() throws DOMException;

    /**
     * The previous one, or null if the start was reached. See the note of the class on what it
     * returns when changing direction.
     *
     * @throws DOMException {@code INVALID_STATE_ERR} if {@link #detach()} was already called
     */
    Node previousNode() throws DOMException;

    /**
     * It lets go of the iterator: the document no longer has to tell it about changes.
     *
     * <p>After this the two walking methods throw. Calling it twice does nothing.
     */
    void detach();
}
