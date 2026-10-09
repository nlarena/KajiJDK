package com.sun.source.doctree;

/**
 * The node of `{@literal}` and of `{@code}`, which are not told apart by the type
 * either: the two escape the HTML inside and the second one besides shows it in monospace.
 */
public interface LiteralTree extends InlineTagTree {

    TextTree getBody();
}
