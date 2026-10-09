package org.xml.sax.ext;

import org.xml.sax.SAXException;

/**
 * KajiLibrary's org.xml.sax.ext.LexicalHandler -- the events SAX2 throws away because they do not
 * change *what* the document says, only *how* it was written.
 *
 * <p>A `ContentHandler` does not find out about comments, nor where a `CDATA` section started and
 * ended, nor that a text came from expanding an entity, nor that there was a DTD. For reading the
 * document that is superfluous: `&lt;a&gt;x&lt;/a&gt;` and `&lt;a&gt;&lt;![CDATA[x]]&gt;&lt;/a&gt;`
 * are the same document. For **rewriting** it, it is not: an editor or a serialiser that loses
 * those boundaries returns a file different from the one it was given. This interface exists for
 * that second kind of consumer.
 *
 * <p>It is not installed with a `setXxxHandler` like the four basic handlers, but with the
 * `http://xml.org/sax/properties/lexical-handler` property of the `XMLReader`. It is an extension:
 * a conforming parser may not recognise it, and then it throws `SAXNotRecognizedException`. That is
 * the practical difference between the core and `ext`.
 *
 * <p>The events really nest, and that is the only way of interpreting them: between `startCDATA`
 * and `endCDATA` the text keeps arriving through `ContentHandler.characters` --no text arrives
 * here--, and between `startEntity` and `endEntity` the events of the contents of the entity
 * arrive. The handler does not receive the contents twice; it receives marks that tell it where
 * they came from.
 *
 * <p>About the order with respect to `startDocument`: `startDTD`/`endDTD` go **after**
 * `startDocument` and **before** the first event of the root element, and everything the
 * `DTDHandler` and the {@link DeclHandler} report falls inside that pair. A comment that is outside
 * the root element arrives all the same, before or after.
 *
 * <p><strong>In KajiLibrary nobody produces these events yet</strong>, because the tree brings no
 * XML parser (see `org.xml.sax.helpers.XMLReaderFactory`). The interface is complete and an
 * external driver installed through the `org.xml.sax.driver` property will be able to use it; what
 * there is not is an emitter of its own.
 */
public interface LexicalHandler {

    /**
     * The `&lt;!DOCTYPE&gt;`. `publicId` and `systemId` may be `null` when the DTD is only
     * internal. Everything that arrives until `endDTD` describes the declaration, not the document.
     */
    void startDTD(String name, String publicId, String systemId) throws SAXException;

    void endDTD() throws SAXException;

    /**
     * The contents of an entity begin. The name is `[dtd]` for the external subset, and carries a
     * `%` in front when it is a parameter entity; both cases are names XML lets nobody else use, so
     * there is no ambiguity with a general entity.
     */
    void startEntity(String name) throws SAXException;

    void endEntity(String name) throws SAXException;

    /** The text inside keeps arriving through `characters`; this only marks the boundary. */
    void startCDATA() throws SAXException;

    void endCDATA() throws SAXException;

    /**
     * A comment, with the array lent just as in `characters`: it is valid inside the call and the
     * parser reuses it afterwards. Whoever wants to keep it copies.
     */
    void comment(char ch[], int start, int length) throws SAXException;
}
