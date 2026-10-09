package org.w3c.dom.stylesheets;

/**
 * A node that links or contains a style sheet: a `<link>` or a `<style>`.
 *
 * <p>The elements implement it, not the document. Hence it is the counterpart of
 * {@link StyleSheet#getOwnerNode}: one goes from the node to the sheet and the other the other way.
 */
public interface LinkStyle {

    /** The sheet this node contributes, or null if it has not loaded yet or is not valid. */
    StyleSheet getSheet();
}
