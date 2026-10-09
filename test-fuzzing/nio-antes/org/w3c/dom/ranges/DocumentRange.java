package org.w3c.dom.ranges;

/**
 * KajiLibrary's org.w3c.dom.ranges.DocumentRange -- the factory of ranges.
 *
 * <p>The {@code Document} implements it, for the same reason as {@code DocumentTraversal}: a range
 * is tied to its document --it adjusts when the document changes-- and for that the document has to
 * know it exists.
 *
 * <p>A {@code Document} that does not support ranges does not implement this interface; one asks
 * with {@code hasFeature("Range", "2.0")}.
 */
public interface DocumentRange {

    /**
     * A new range, with both ends set at the start of the document -- that is, collapsed.
     */
    Range createRange();
}
