package org.xml.sax;

/**
 * KajiLibrary's org.xml.sax.ContentHandler -- where the events of the document land.
 *
 * <p>It is the main handler of {@link XMLReader}, the one that receives the structure and the text.
 * What it adds over the other three is that its calls are **nested and ordered**: everything of the
 * document goes between `startDocument` and `endDocument`, and each `startElement` has its
 * `endElement` even if a recoverable error happened in between. A typical handler is therefore a
 * state machine with a stack.
 *
 * <p><strong>The three traps of this interface, which do not show in the signatures:</strong>
 *
 * <p>One: `characters` may be called **several times for one same piece of text**. The parser may
 * cut wherever suits it --at the end of its buffer, when expanding an entity-- and is not obliged
 * to join. Whoever does `if (text.equals("hello"))` inside `characters` has a bug that shows up
 * with large documents and not with those of the test. The right thing is to accumulate in a
 * `StringBuilder` and look at it in `endElement`.
 *
 * <p>Two: the `char[]` that arrives **does not belong to the handler**. The parser reuses it on the
 * next call. Keeping the reference instead of copying the range `[start, start+length)` gives
 * corrupt data later, somewhere else, with nothing pointing to the cause.
 *
 * <p>Three: the {@link Attributes} object of `startElement` **is only valid during that call**. To
 * keep it, it has to be copied, which is precisely what `AttributesImpl` exists for.
 *
 * <p>The methods declare `throws SAXException` because it is the only way a handler has of stopping
 * the parse: the exception goes up through `parse`.
 */
public interface ContentHandler {

    /**
     * It is called before `startDocument`, if it is called at all.
     *
     * <p>The object that arrives must **not** be kept in order to consult it later: its coordinates
     * change by themselves as the parser advances, and outside the handler that reads them at the
     * moment they mean nothing.
     */
    void setDocumentLocator(Locator locator);

    void startDocument() throws SAXException;

    /**
     * The declaration `&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;`, if there was
     * one.
     *
     * <p>It is `default` and not abstract because it was added long after the rest of the
     * interface: making it abstract would have broken every handler written until then, which are
     * many. The default body does nothing, which is exactly what they did before.
     *
     * @param encoding `null` if the declaration did not bring it.
     * @param standalone `null` if it was not there; otherwise, `"yes"` or `"no"`.
     */
    default void declaration(String version, String encoding, String standalone)
            throws SAXException {
    }

    void endDocument() throws SAXException;

    /**
     * A prefix starts being bound to a URI.
     *
     * <p>It goes **before** the `startElement` that declares it, not inside, so that the handler
     * already has the map built when the element arrives. The default prefix (`xmlns="..."`)
     * arrives as an empty string, not as `null`.
     */
    void startPrefixMapping(String prefix, String uri) throws SAXException;

    /** It goes **after** the matching `endElement`, in the reverse order of opening. */
    void endPrefixMapping(String prefix) throws SAXException;

    /**
     * @param uri empty if the element has no namespace, or if the parser does not process them.
     * @param localName empty if the parser does not process namespaces.
     * @param qName the name as it was written; it may come empty if the parser does not report it.
     *        That the three may be empty depending on the configuration is what forces one to look
     *        at `qName` in some parsers and at `localName` in others.
     */
    void startElement(String uri, String localName, String qName, Attributes atts)
            throws SAXException;

    void endElement(String uri, String localName, String qName) throws SAXException;

    /** See above: the array is reused and the text may come split. */
    void characters(char[] ch, int start, int length) throws SAXException;

    /**
     * White space the DTD says is not content, typically indentation.
     *
     * <p>Without validation a parser cannot know which it is and sends it all through `characters`.
     * That this method is never called does not mean there was no indentation.
     */
    void ignorableWhitespace(char[] ch, int start, int length) throws SAXException;

    /**
     * The `&lt;?xml ...?&gt;` declaration does not arrive through here: `declaration` is for that.
     */
    void processingInstruction(String target, String data) throws SAXException;

    /**
     * The parser skipped an entity instead of expanding it.
     *
     * <p>It happens when it does not validate and did not read the external DTD, or when the
     * resolution of entities was switched off. The name of a parameter entity arrives with a `%` in
     * front.
     */
    void skippedEntity(String name) throws SAXException;
}
