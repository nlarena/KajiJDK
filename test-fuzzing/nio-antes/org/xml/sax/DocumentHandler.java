package org.xml.sax;

import org.xml.sax.AttributeList;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

// KajiLibrary's org.xml.sax.DocumentHandler -- the predecessor of ContentHandler in SAX1.
//
// The differences are exactly two, and both are about namespaces: an element arrives with one
// single name instead of the triple (uri, localName, qName), and there are no prefix mapping
// events. Everything else --the order of the calls, the rule that `characters` may come split,
// the rule that the char[] is a lent window-- is the same.
//
// helpers.XMLReaderAdapter turns a SAX2 XMLReader into one of these; helpers.ParserAdapter goes
// the other way. Deprecated in the JDK in favour of ContentHandler; see AttributeList for why
// @Deprecated is not written here.
public interface DocumentHandler {

    // The live oracle of position; see ContentHandler.setDocumentLocator.
    void setDocumentLocator(Locator locator);

    // The start of the document.
    void startDocument() throws SAXException;

    // The end of the document.
    void endDocument() throws SAXException;

    // The start of an element, named as it was written in the document.
    void startElement(String name, AttributeList atts) throws SAXException;

    // The end of an element.
    void endElement(String name) throws SAXException;

    // Character data; it may come split across several calls.
    void characters(char[] ch, int start, int length) throws SAXException;

    // White space of the content of an element, reported by a validating parser.
    void ignorableWhitespace(char[] ch, int start, int length) throws SAXException;

    // A processing instruction.
    void processingInstruction(String target, String data) throws SAXException;
}
