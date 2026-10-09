package org.w3c.dom.xpath;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.xpath.XPathResult -- what an expression returned.
 *
 * <p>An object with ten possible types and one accessor per type: reading the wrong one throws. One
 * asks first with {@link #getResultType()}, unless a concrete type was asked for when evaluating
 * --and then it is already known which it is--.
 *
 * <h2>Iterator or snapshot: the choice that matters</h2>
 *
 * <p>The four node-set types split into two pairs, and the difference is not one of style:
 *
 * <ul>
 *   <li><b>iterator</b> -- lazy. If the document changes while it is being walked, the iterator is
 *       <b>invalidated</b> and {@link #iterateNext()} throws. One asks with
 *       {@link #getInvalidIteratorState()}.
 *   <li><b>snapshot</b> -- materialised on evaluating. It survives the changes, but the nodes it
 *       has inside may have stopped being in the document.
 * </ul>
 *
 * <p>And the "ordered" also costs: asking for document order may be more expensive, so the
 * unordered version exists for when the order does not matter.
 */
public interface XPathResult {

    /** Whatever the expression naturally gives. */
    short ANY_TYPE = 0;

    /** A number. */
    short NUMBER_TYPE = 1;

    /** A string. */
    short STRING_TYPE = 2;

    /** A boolean. */
    short BOOLEAN_TYPE = 3;

    /** A lazy node set, with no guaranteed order. */
    short UNORDERED_NODE_ITERATOR_TYPE = 4;

    /** A lazy node set, in document order. */
    short ORDERED_NODE_ITERATOR_TYPE = 5;

    /** A materialised node set, with no guaranteed order. */
    short UNORDERED_NODE_SNAPSHOT_TYPE = 6;

    /** A materialised node set, in document order. */
    short ORDERED_NODE_SNAPSHOT_TYPE = 7;

    /** Any one single node of the set. */
    short ANY_UNORDERED_NODE_TYPE = 8;

    /** The first node in document order. */
    short FIRST_ORDERED_NODE_TYPE = 9;

    /** Which of the ten it is. */
    short getResultType();

    /** @throws XPathException {@code TYPE_ERR} if the result is not a number */
    double getNumberValue() throws XPathException;

    /** @throws XPathException {@code TYPE_ERR} if the result is not a string */
    String getStringValue() throws XPathException;

    /** @throws XPathException {@code TYPE_ERR} if the result is not a boolean */
    boolean getBooleanValue() throws XPathException;

    /** @throws XPathException {@code TYPE_ERR} if the result is not a single node */
    Node getSingleNodeValue() throws XPathException;

    /**
     * Whether the document changed and the iterator stopped serving. It only applies to the lazy
     * types.
     */
    boolean getInvalidIteratorState();

    /** @throws XPathException {@code TYPE_ERR} if the result is not a snapshot */
    int getSnapshotLength() throws XPathException;

    /**
     * The next node, or null if it ran out.
     *
     * @throws XPathException {@code TYPE_ERR} if it is not an iterator
     * @throws DOMException {@code INVALID_STATE_ERR} if the document changed; see the note of the
     *     class
     */
    Node iterateNext() throws XPathException, DOMException;

    /**
     * The node at that position of the snapshot, or null if the index goes past.
     *
     * @throws XPathException {@code TYPE_ERR} if it is not a snapshot
     */
    Node snapshotItem(int index) throws XPathException;
}
