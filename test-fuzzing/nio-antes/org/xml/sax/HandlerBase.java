package org.xml.sax;

// KajiLibrary's org.xml.sax.HandlerBase -- the SAX1 base of the "override only what you care
// about" style.
//
// It implements the four SAX1 handler interfaces at once (EntityResolver, DTDHandler,
// DocumentHandler, ErrorHandler) with bodies that do nothing, so whoever only wants startElement
// writes one method instead of fourteen. Its replacement in SAX2 is
// org.xml.sax.helpers.DefaultHandler, which does the same job for ContentHandler; that is why this
// class is deprecated in the JDK, and it is kept here because the contract still lists it.
//
// There are two default behaviours worth naming because they are decisions and not omissions:
//
//   - resolveEntity returns null, which tells the parser "open the system identifier yourself",
//     that is, the ordinary behaviour when nobody intervenes.
//   - error() and warning() return quietly, so a non-fatal problem is accepted silently; only
//     fatalError() throws, and it rethrows the exception it was given. That asymmetry is the SAX
//     rule: a fatal error has to stop the analysis, a recoverable one does not.
//
// Note that here resolveEntity is declared throwing only SAXException and not IOException, unlike
// EntityResolver.resolveEntity, which allows both. Narrowing the set of thrown exceptions in an
// override is legal, and the JDK narrows it here; DefaultHandler does not.
public class HandlerBase
        implements EntityResolver, DTDHandler, DocumentHandler, ErrorHandler {

    public HandlerBase() {
    }

    // Null means "no substitution": use the system identifier as it came.
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException {
        return null;
    }

    public void notationDecl(String name, String publicId, String systemId) {
    }

    public void unparsedEntityDecl(String name, String publicId,
                                   String systemId, String notationName) {
    }

    public void setDocumentLocator(Locator locator) {
    }

    public void startDocument() throws SAXException {
    }

    public void endDocument() throws SAXException {
    }

    public void startElement(String name, AttributeList attributes)
            throws SAXException {
    }

    public void endElement(String name) throws SAXException {
    }

    public void characters(char ch[], int start, int length)
            throws SAXException {
    }

    public void ignorableWhitespace(char ch[], int start, int length)
            throws SAXException {
    }

    public void processingInstruction(String target, String data)
            throws SAXException {
    }

    public void warning(SAXParseException e) throws SAXException {
    }

    public void error(SAXParseException e) throws SAXException {
    }

    // The only one that does not stay quiet: a fatal error ends the analysis by definition, so
    // swallowing it would leave the parser with nothing to do and the caller with no news.
    public void fatalError(SAXParseException e) throws SAXException {
        throw e;
    }
}
