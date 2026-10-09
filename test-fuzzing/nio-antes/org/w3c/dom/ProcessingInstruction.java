package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.ProcessingInstruction -- a {@code <?target data?>}.
 *
 * <p>It is the mechanism XML gives for putting into the document something aimed at **one**
 * application without it being content: the everyday example is
 * {@code <?xml-stylesheet type="text/xsl" href="v.xsl"?>}. The target names the recipient and the
 * data are opaque --the parser does not interpret them, not even as key-value pairs, even though
 * almost everybody writes them that way.
 *
 * <p>Note that it does **not** extend {@link CharacterData} even though it has text: the data of a
 * PI are not content of the document, they do not count for the {@link Node#getTextContent} of the
 * parent, and that is why it has its own pair of accessors instead of inheriting the eight text
 * editing operations.
 *
 * <p>The {@code <?xml version="1.0"?>} declaration at the start is **not** a PI and does not appear
 * as a node: it is part of the syntax of the document and is reached through
 * {@link Document#getXmlVersion} and company.
 *
 * <p>The interface is declared whole.
 */
public interface ProcessingInstruction extends Node {

    /** Who it is aimed at; it is also what {@link Node#getNodeName} returns. */
    public String getTarget();

    /** The text that follows the target up to the {@code ?>}, uninterpreted. */
    public String getData();

    /**
     * It changes the data.
     *
     * @throws DOMException {@code NO_MODIFICATION_ALLOWED_ERR} if the node is read-only
     */
    public void setData(String data) throws DOMException;
}
