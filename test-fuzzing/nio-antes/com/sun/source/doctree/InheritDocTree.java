package com.sun.source.doctree;

/**
 * The node of `{@inheritDoc}`, which brings the overridden method's documentation.
 *
 * <p>{@link #getSupertype} is `null` in the form with no argument -- the classic one -- and
 * returns the reference when `{@inheritDoc Supertype}` was written, which exists in order to
 * disambiguate when there is more than one supertype with documentation.
 */
public interface InheritDocTree extends InlineTagTree {

    /** The supertype to inherit from, or `null` if which one was not said. */
    default ReferenceTree getSupertype() {
        return null;
    }
}
