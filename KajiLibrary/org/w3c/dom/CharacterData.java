package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.CharacterData -- what the nodes that are a string of characters share:
 * `Text`, `Comment` and `CDATASection`.
 *
 * <p>What it adds over `Node` are the editing operations on that string --inserting, deleting,
 * replacing by range-- which exist so as not to have to read the whole text, modify it in Java and
 * write it back. On a node of several megabytes the difference shows.
 *
 * <p><strong>The offsets are in 16-bit units, not in code points.</strong> `getLength()` counts the
 * same as `String.length()`, so a character outside the basic plane counts as two and a badly
 * calculated `deleteData` can split a surrogate pair in half. The standard inherits it from
 * `DOMString`, which was defined as UTF-16 and not as text.
 */
public interface CharacterData extends Node {

    String getData() throws DOMException;

    void setData(String data) throws DOMException;

    /** In 16-bit units. */
    int getLength();

    /**
     * @throws DOMException with `INDEX_SIZE_ERR` if `offset` is out of range or `count` is
     *         negative. That `offset + count` goes past the end is **not** an error: it is trimmed.
     */
    String substringData(int offset, int count) throws DOMException;

    void appendData(String arg) throws DOMException;

    void insertData(int offset, String arg) throws DOMException;

    void deleteData(int offset, int count) throws DOMException;

    /**
     * It is not equivalent to `deleteData` plus `insertData`: it is one operation and one mutation.
     */
    void replaceData(int offset, int count, String arg) throws DOMException;
}
