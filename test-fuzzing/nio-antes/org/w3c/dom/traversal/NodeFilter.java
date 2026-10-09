package org.w3c.dom.traversal;

import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.traversal.NodeFilter -- which nodes are seen when walking a document.
 *
 * <h2>Three answers, not two</h2>
 *
 * <p>It is the only thing to understand about this interface, and the difference between the two
 * negatives is the one that gets forgotten:
 *
 * <ul>
 *   <li>{@link #FILTER_ACCEPT} -- the node is seen.
 *   <li>{@link #FILTER_SKIP} -- the node is not seen, <b>but its children are</b>.
 *   <li>{@link #FILTER_REJECT} -- the node is not seen <b>and neither is its whole subtree</b>.
 * </ul>
 *
 * <p>And there is an asymmetry: a {@link NodeIterator} treats {@code FILTER_REJECT} as {@code
 * FILTER_SKIP}, because it walks a flat list and has no subtree to prune. The distinction only
 * changes something in a {@link TreeWalker}.
 *
 * <h2>The filter and whatToShow are two sieves in series</h2>
 *
 * <p>The {@code whatToShow} of the walk is applied <b>first</b>: a node of a type that is not in
 * the mask does not even reach the filter. That is why a filter that wants to see comments is not
 * enough -- {@link #SHOW_COMMENT} has to be asked for as well.
 */
public interface NodeFilter {

    /** The node is seen. */
    short FILTER_ACCEPT = 1;

    /** The node is not seen, and neither is its subtree. See the note of the class. */
    short FILTER_REJECT = 2;

    /** The node is not seen, but its children are. */
    short FILTER_SKIP = 3;

    /** All the types of node. */
    int SHOW_ALL = 0xFFFFFFFF;

    /** Elements. */
    int SHOW_ELEMENT = 0x00000001;

    /** Attributes. It only makes sense if the root of the walk is an attribute. */
    int SHOW_ATTRIBUTE = 0x00000002;

    /** Text nodes. */
    int SHOW_TEXT = 0x00000004;

    /** CDATA sections. */
    int SHOW_CDATA_SECTION = 0x00000008;

    /** Entity references. */
    int SHOW_ENTITY_REFERENCE = 0x00000010;

    /** Entities. Only if the root is the entity. */
    int SHOW_ENTITY = 0x00000020;

    /** Processing instructions. */
    int SHOW_PROCESSING_INSTRUCTION = 0x00000040;

    /** Comments. */
    int SHOW_COMMENT = 0x00000080;

    /** The document node. */
    int SHOW_DOCUMENT = 0x00000100;

    /** The document type declaration. */
    int SHOW_DOCUMENT_TYPE = 0x00000200;

    /** Document fragments. */
    int SHOW_DOCUMENT_FRAGMENT = 0x00000400;

    /** Notations. Only if the root is the notation. */
    int SHOW_NOTATION = 0x00000800;

    /**
     * It decides whether that node is seen.
     *
     * @return one of the three {@code FILTER_*} constants
     */
    short acceptNode(Node n);
}
