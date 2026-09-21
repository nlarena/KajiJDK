package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.Text -- the loose text between tags.
 *
 * <p>What it adds over `CharacterData` is the notion of **logical text**: one same paragraph of the
 * document may be split into several adjacent `Text` and `CDATASection` nodes --it happens when
 * there was an entity reference in the middle that was expanded-- and `getWholeText()` returns the
 * concatenation of all of them, which is what the author of the XML wrote. `replaceWholeText` does
 * the inverse operation: it replaces the whole group by a single node.
 */
public interface Text extends CharacterData {

    /**
     * It splits this node into two siblings and returns the **second**, the one that keeps what
     * goes from `offset` onwards.
     *
     * @throws DOMException with `INDEX_SIZE_ERR` if the offset is out of range.
     */
    Text splitText(int offset) throws DOMException;

    /**
     * Whether this node is **ignorable** white space, that is indentation the DTD says is not
     * content.
     *
     * <p>With no DTD nor schema there is no way of knowing, and then it returns `false`: "it is not
     * ignorable" is not the same as "it is not known", but the interface only has a `boolean` to
     * say it.
     */
    boolean isElementContentWhitespace();

    /** The text of this node plus that of its adjacent text siblings. */
    String getWholeText();

    /**
     * It returns the node that was left: this one, a new one, or `null` if the content was empty.
     */
    Text replaceWholeText(String content) throws DOMException;
}
