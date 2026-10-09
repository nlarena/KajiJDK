package org.w3c.dom.ls;

import org.w3c.dom.traversal.NodeFilter;

/**
 * KajiLibrary's org.w3c.dom.ls.LSSerializerFilter -- it decides which nodes go out when
 * serialising.
 *
 * <p>It reuses {@code NodeFilter} instead of defining its own, which is the interesting design
 * decision of the type: writing a document is walking it, and walking it with a filter was already
 * solved in {@code org.w3c.dom.traversal}. The only thing it adds is redeclaring {@link
 * #getWhatToShow} to document a real difference with Traversal: here {@code SHOW_ATTRIBUTE} means
 * that the {@code Attr} nodes <b>are</b> shown and passed to the filter, and a node that is not
 * shown is serialised automatically. The note said the opposite --that the mask cannot exclude
 * attributes and that they are always serialised--; the specification, and the JDK's javadoc for
 * this method, say what is written here.
 *
 * <p>The inherited {@code FILTER_SKIP} has the same meaning as in {@link LSParserFilter}: the node
 * does not go out but its children do.
 */
public interface LSSerializerFilter extends NodeFilter {

    /**
     * Which types of node are passed to it.
     *
     * <p>A node that is not shown to the filter is serialised automatically. Unlike Traversal,
     * {@code SHOW_ATTRIBUTE} shows the {@code Attr} nodes to the filter, so an attribute can be
     * filtered out; see the note of the class.
     */
    int getWhatToShow();
}
