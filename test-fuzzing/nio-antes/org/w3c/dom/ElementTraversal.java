package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.ElementTraversal -- walking the tree seeing **only** elements.
 *
 * <p>It is the youngest of the package and the only one that does not come from DOM Core: it is a
 * separate W3C recommendation, "Element Traversal". It answers a concrete everyday annoyance: in an
 * indented XML, between two sibling elements there is a {@link Text} node of spaces, so
 * {@link Node#getFirstChild} almost never returns the element one wanted and everybody ends up
 * writing the same loop of skipping text and comments.
 *
 * <p>Note that it does **not** extend {@link Node} nor {@link Element}: it is a loose interface an
 * implementation adds to its element nodes. That is why it may appear in an {@code instanceof} on
 * something already known to be an {@link Element}, and why not every {@code Element} has it.
 *
 * <p>Four of its five methods are the filtered view of four of the navigation methods of {@link
 * Node} --the parent has no counterpart, since the parent of an element is always an element or the
 * document--, and {@link #getChildElementCount} is what {@code getChildNodes().getLength()} would
 * be counting only elements. The note said "the five methods are the filtered view of the five of
 * Node", which counts the count as navigation.
 *
 * <p>The interface is declared whole.
 */
public interface ElementTraversal {

    /** The first child that is an element, or {@code null}. */
    public Element getFirstElementChild();

    /** The last child that is an element, or {@code null}. */
    public Element getLastElementChild();

    /** The previous sibling that is an element, or {@code null}. */
    public Element getPreviousElementSibling();

    /** The next sibling that is an element, or {@code null}. */
    public Element getNextElementSibling();

    /** How many children are elements. */
    public int getChildElementCount();
}
