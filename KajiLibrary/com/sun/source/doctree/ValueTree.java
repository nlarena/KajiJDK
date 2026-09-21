package com.sun.source.doctree;

/**
 * The node of `{@value}`, which substitutes a constant's value.
 * {@link #getFormat} is the optional format that may be given to it.
 */
public interface ValueTree extends InlineTagTree {

    ReferenceTree getReference();

    default TextTree getFormat() {
        return null;
    }
}
