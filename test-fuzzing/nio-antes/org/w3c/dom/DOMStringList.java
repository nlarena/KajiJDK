package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMStringList -- an ordered list of strings.
 *
 * <p>{@link DOMConfiguration#getParameterNames} uses it. It is the same minimal shape as
 * {@link NodeList} but for strings, and it exists for the same reason as that one: the DOM was
 * specified in IDL and could not return a {@code java.util.List}.
 *
 * <p>The note said that {@link #contains} compares **case-insensitively** because DOM parameter
 * names are case-insensitive. The specification does not say that: it defines {@code contains} only
 * as "whether the string is part of this list". Parameter names are case-insensitive in
 * {@link DOMConfiguration}, which is a different interface.
 *
 * <p>The interface is declared whole.
 */
public interface DOMStringList {

    /** The string at that position, or {@code null} if the index went out of range. */
    public String item(int index);

    /** How many strings there are. */
    public int getLength();

    /** Whether that string is in the list. See the note of the class on case. */
    public boolean contains(String str);
}
