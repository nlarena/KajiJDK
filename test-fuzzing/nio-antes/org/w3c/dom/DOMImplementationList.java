package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMImplementationList -- an ordered list of implementations.
 *
 * <p>{@link DOMImplementationSource#getDOMImplementationList} returns it when more than one
 * implementation says it supports what was asked for. The same minimal shape as {@link NodeList}
 * --and indexed from zero-- and for the same reason: the DOM does not rest on the collections of
 * any language.
 *
 * <p>The interface is declared whole.
 */
public interface DOMImplementationList {

    /** The implementation at that position, or {@code null} if the index went out of range. */
    public DOMImplementation item(int index);

    /** How many there are. */
    public int getLength();
}
