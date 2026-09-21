package org.w3c.dom.css;

/**
 * A `counter()` or `counters()` of the `content` property.
 *
 * <p>`getSeparator` is what tells the two forms apart: `counter()` has no separator and returns
 * null; `counters()` does have one, and it is what is interleaved between the levels of a nested
 * counter.
 */
public interface Counter {

    /** The name of the counter. */
    String getIdentifier();

    /** The numbering style --`decimal`, `lower-roman`--. */
    String getListStyle();

    /** The separator of `counters()`, or null if it is a plain `counter()`. */
    String getSeparator();
}
