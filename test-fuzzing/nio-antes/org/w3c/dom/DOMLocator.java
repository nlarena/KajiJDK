package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMLocator -- where something happened, said in four ways at once.
 *
 * <p>{@link DOMError#getLocation} returns it. That there are four coordinates is not redundancy:
 * each one serves a different consumer and **not all are always available**. Line and column are
 * for showing the problem to a person; the byte offset serves whoever has the original file open;
 * the offset in UTF-16 units serves whoever has the text already decoded in memory, which is
 * something else. And {@link #getRelatedNode} is the only useful one when the document did not come
 * from any text but was built node by node, a case in which the previous three return {@code -1}.
 *
 * <p>That {@code -1} is the convention for "not known" in the four numeric ones. The numbering of
 * line and column starts at 1.
 *
 * <p>The interface is declared whole.
 */
public interface DOMLocator {

    /** The line, counting from 1, or {@code -1} if it is not known. */
    public int getLineNumber();

    /** The column, counting from 1, or {@code -1} if it is not known. */
    public int getColumnNumber();

    /** The offset in bytes inside the input, or {@code -1}. */
    public int getByteOffset();

    /** The offset in UTF-16 code units, or {@code -1}. */
    public int getUtf16Offset();

    /** The node it points at, or {@code null}. */
    public Node getRelatedNode();

    /** The URI it came from, or {@code null}. */
    public String getUri();
}
