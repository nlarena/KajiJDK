package org.w3c.dom.ranges;

import org.w3c.dom.DOMException;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.ranges.Range -- a stretch of the document, which may start and end in
 * the middle of a text.
 *
 * <p>It is what is needed to represent a <b>selection</b>. A {@code Node} is not enough: when
 * somebody selects with the mouse, what is selected starts halfway through one paragraph and ends
 * halfway through another, and there is no node that is that.
 *
 * <h2>An end is a (container, offset) pair</h2>
 *
 * <p>And the offset means two different things according to the container, which is what confuses:
 *
 * <ul>
 *   <li>If the container is a <b>text</b> node, it is a <b>character</b> index.
 *   <li>If it is any other, it is a <b>child</b> index: how many children are left before the end.
 * </ul>
 *
 * <p>Hence a valid offset goes up to {@code length} inclusive and not up to {@code length - 1}: the
 * end may be <b>after</b> the last character or the last child.
 *
 * <h2>Extracting is not cloning</h2>
 *
 * <p>The three methods that work on the contents look alike and do different things: {@link
 * #cloneContents()} copies and does not touch the document, {@link #extractContents()} <b>takes it
 * out</b> and returns it, and {@link #deleteContents()} takes it out and returns nothing. The last
 * two leave the range collapsed where the contents were.
 *
 * <p>A range <b>stays alive</b> when the document changes: it adjusts, just like a
 * {@link org.w3c.dom.traversal.NodeIterator}. That is why it also has {@link #detach()}.
 */
public interface Range {

    /** It compares the start of this range with the start of the other. */
    short START_TO_START = 0;

    /** It compares the <b>end</b> of this range with the <b>start</b> of the other. */
    short START_TO_END = 1;

    /** It compares the end of this one with the end of the other. */
    short END_TO_END = 2;

    /** It compares the <b>start</b> of this one with the <b>end</b> of the other. */
    short END_TO_START = 3;

    /** The node where it starts. */
    Node getStartContainer() throws DOMException;

    /** Where it starts inside it. See the note of the class on what it means. */
    int getStartOffset() throws DOMException;

    /** The node where it ends. */
    Node getEndContainer() throws DOMException;

    /** Where it ends inside it. */
    int getEndOffset() throws DOMException;

    /** Whether the two ends coincide, that is whether the range is empty. */
    boolean getCollapsed() throws DOMException;

    /** The nearest common ancestor of the two ends. */
    Node getCommonAncestorContainer() throws DOMException;

    /**
     * It moves the start.
     *
     * <p>If the new start ends up <b>after</b> the end, the range collapses there instead of being
     * left inverted. It is from the standard and it keeps an impossible range from existing.
     *
     * @throws RangeException {@code INVALID_NODE_TYPE_ERR} if that node cannot contain an end
     * @throws DOMException {@code INDEX_SIZE_ERR} if the offset goes past
     */
    void setStart(Node refNode, int offset) throws RangeException, DOMException;

    /** It moves the end. The same holds as for {@link #setStart}, the other way round. */
    void setEnd(Node refNode, int offset) throws RangeException, DOMException;

    /** It puts the start just before that node. */
    void setStartBefore(Node refNode) throws RangeException, DOMException;

    /** It puts the start just after that node. */
    void setStartAfter(Node refNode) throws RangeException, DOMException;

    /** It puts the end just before that node. */
    void setEndBefore(Node refNode) throws RangeException, DOMException;

    /** It puts the end just after that node. */
    void setEndAfter(Node refNode) throws RangeException, DOMException;

    /**
     * It joins the two ends.
     *
     * @param toStart whether it collapses to the start; if it is false, to the end
     */
    void collapse(boolean toStart) throws DOMException;

    /** It makes the range exactly that node, the node itself included. */
    void selectNode(Node refNode) throws RangeException, DOMException;

    /** It makes the range the <b>contents</b> of that node, without the node. */
    void selectNodeContents(Node refNode) throws RangeException, DOMException;

    /**
     * It compares one end of this range with one of the other.
     *
     * @param how which with which: one of the four constants. Careful with {@link #START_TO_END}
     *     and {@link #END_TO_START}, which cross the ends
     * @return -1, 0 or 1
     */
    short compareBoundaryPoints(short how, Range sourceRange) throws DOMException;

    /** It deletes the contents from the document. The range is left collapsed there. */
    void deleteContents() throws DOMException;

    /** It <b>takes it out</b> of the document and returns it. See the note of the class. */
    DocumentFragment extractContents() throws DOMException;

    /** It <b>copies</b> it without touching the document. */
    DocumentFragment cloneContents() throws DOMException;

    /**
     * It puts that node at the start of the range.
     *
     * <p>If the start is halfway through a text, the text is <b>split</b> to make room for it.
     */
    void insertNode(Node newNode) throws DOMException, RangeException;

    /**
     * It wraps the contents of the range with that node.
     *
     * @throws RangeException {@code BAD_BOUNDARYPOINTS_ERR} if the range splits a node in half:
     *     wrapping something that starts inside an element and ends outside it would give an
     *     impossible tree
     */
    void surroundContents(Node newParent) throws DOMException, RangeException;

    /** An independent copy of this range. */
    Range cloneRange() throws DOMException;

    /** The text of the contents, with no markup. */
    String toString();

    /**
     * It lets go of the range: the document no longer has to adjust it. After that, everything else
     * throws.
     */
    void detach() throws DOMException;
}
