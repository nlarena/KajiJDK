package com.sun.source.doctree;

/**
 * A reference to a Java element, such as the one that goes inside a `{@link}`.
 *
 * <p>It returns the **raw signature**, unresolved: turning `Foo#bar(int)` into the method it
 * names needs the compilation context, which this tree does not have. Resolving it is
 * `DocTrees`' work, not this node's.
 */
public interface ReferenceTree extends DocTree {

    /** The signature just as it was written, unresolved. */
    String getSignature();
}
