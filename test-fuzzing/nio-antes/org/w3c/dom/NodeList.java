package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.NodeList -- an ordered collection of nodes, indexed from zero.
 *
 * <p>What it adds over a Java `List`: **nothing**, and that is precisely the point. The DOM was
 * specified for several languages at once and could not rest on the library of any of them, so it
 * declared its own minimal collection. Hence it is neither `Iterable` nor has a `size()`.
 *
 * <p><strong>What it does have and a `List` does not: it is almost always live.</strong> The list
 * `getChildNodes()` or `getElementsByTagName()` returns reflects the tree at the moment it is
 * queried, not a snapshot of when it was asked for. That is why the loop `for (int i = 0; i &lt;
 * l.getLength(); i++)` that deletes nodes inside skips half of them: each deletion shifts the rest
 * one place. It is the classic mistake of the DOM and there is nothing in the signature that warns.
 */
public interface NodeList {

    /** `null` --not an exception-- if the index is out of range. */
    Node item(int index);

    int getLength();
}
