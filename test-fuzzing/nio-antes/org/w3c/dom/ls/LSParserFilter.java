package org.w3c.dom.ls;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.ls.LSParserFilter -- it decides what gets into the tree, while parsing.
 *
 * <p>What it is really for is <b>not building</b> what is of no interest. A document of a hundred
 * megabytes of which only a few elements matter can be parsed without the whole tree ever existing,
 * and that is the only way of reading it with DOM without running out of memory.
 *
 * <h2>The two methods are two moments</h2>
 *
 * <p>{@link #startElement} is called on seeing the opening tag, with the element <b>empty</b>: it
 * has no children nor text yet. {@link #acceptNode} is called with the node already complete. The
 * difference is the whole point: rejecting in {@code startElement} avoids building the subtree, and
 * rejecting in {@code acceptNode} only throws it away after having built it. One can only decide
 * early with what there is in the tag --the name and the attributes-- and that is why it is as well
 * to look there.
 *
 * <p>{@link #FILTER_SKIP} and {@link #FILTER_REJECT} are not the same either (the note linked
 * `#SKIP` and `#REJECT`, which do not exist): skipping discards the element but <b>keeps</b> its
 * children, which move up one level; rejecting takes the whole subtree.
 *
 * <p>{@link #getWhatToShow} limits which types of node it is asked about, with the masks of
 * {@code NodeFilter}. It serves for not paying one call for each text node of a document when the
 * filter only looks at elements.
 */
public interface LSParserFilter {

    /** The node gets in as it is. */
    short FILTER_ACCEPT = 1;

    /** The node and its whole subtree are discarded. */
    short FILTER_REJECT = 2;

    /** The node is discarded but its children move up one level. See the note of the class. */
    short FILTER_SKIP = 3;

    /** The analysis is cut short; the document is left incomplete. */
    short FILTER_INTERRUPT = 4;

    /**
     * On opening the tag, with the element still empty.
     *
     * <p>It is the moment when it is as well to reject; see the note of the class.
     *
     * @return one of the four constants
     */
    short startElement(Element elementArg);

    /**
     * With the node already built.
     *
     * <p>The note said that {@link #FILTER_SKIP} is not valid for elements already accepted in
     * {@link #startElement}; the specification states no such restriction -- it gives it the same
     * meaning here as there: the node is skipped and replaced by its children.
     *
     * @return one of the four constants
     */
    short acceptNode(Node nodeArg);

    /** Which types of node are passed to it, with the masks of {@code NodeFilter}. */
    int getWhatToShow();
}
