package org.w3c.dom.html;

import org.w3c.dom.Node;

/**
 * A list of elements indexable by position and by name.
 *
 * <p>It is **live**, like those of {@link HTMLDocument}: what it returns reflects the tree at the
 * moment of the query.
 *
 * <p>`namedItem` looks first by `id` and then by `name`, in that order. It matters when the two
 * attributes exist and do not match: the `id` wins.
 */
public interface HTMLCollection {

    /** The count. */
    int getLength();

    /** The element at that position, or null if the index is out of range. */
    Node item(int index);

    /** The element with that `id`, or failing that with that `name`; null if there is none. */
    Node namedItem(String name);
}
