package org.xml.sax;

/**
 * KajiLibrary's org.xml.sax.Attributes -- the attributes of an element, as they arrive at
 * `ContentHandler.startElement`.
 *
 * <p>It is an indexed list **and** a map with two different keys at the same time: it can be walked
 * by position (`getURI(i)`, `getValue(i)`) or looked up by qualified name (`getValue(String)`) or
 * by (URI, local name) (`getValue(String, String)`). The three views are over the same thing and
 * all three are there because a parser may be configured to report only one of the two forms of
 * name; which one works depends on the `namespaces` and `namespace-prefixes` *features* of the
 * reader.
 *
 * <p><strong>The object is lent and is valid only inside the call.</strong> The parser reuses it
 * for the next element. Keeping it is the classic SAX bug: the handler is left with a reference
 * that later describes another element. To keep it, it has to be copied.
 *
 * <p><strong>The order means nothing.</strong> XML gives the attributes no order and a parser may
 * hand them over in any; code that depends on the index to identify which is which breaks on
 * changing implementation.
 *
 * <p>The types `getType` returns are those of the DTD --`CDATA`, `ID`, `IDREF`, `IDREFS`,
 * `NMTOKEN`, `NMTOKENS`, `ENTITY`, `ENTITIES`, `NOTATION`-- and with no DTD they are all `CDATA`:
 * it is what the standard says for "not known", not a type that was found out.
 */
public interface Attributes {

    int getLength();

    /** Empty string if it has no namespace or if the parser does not process them; never `null`. */
    String getURI(int index);

    String getLocalName(int index);

    /** The name as it was written, with its prefix. Empty if the parser does not report it. */
    String getQName(int index);

    String getType(int index);

    /** Entities already expanded and white space already normalised, as XML demands. */
    String getValue(int index);

    /** -1 if it is not there. The four `getXxx(int)` return `null` with an index out of range. */
    int getIndex(String uri, String localName);

    int getIndex(String qName);

    /** `null` if there is no attribute with that name; **not** an exception. */
    String getType(String uri, String localName);

    String getType(String qName);

    String getValue(String uri, String localName);

    String getValue(String qName);
}
