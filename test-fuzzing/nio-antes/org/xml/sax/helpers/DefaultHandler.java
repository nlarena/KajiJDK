package org.xml.sax.helpers;

import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

// KajiLibrary's org.xml.sax.helpers.DefaultHandler -- the SAX2 base class people really extend.
//
// It implements EntityResolver, DTDHandler, ContentHandler and ErrorHandler with empty bodies, so
// the typical "I only care about startElement and characters" handler is two methods and not
// seventeen. One same instance is passed to setContentHandler, setErrorHandler, setDTDHandler and
// setEntityResolver and the parser is left fully connected.
//
// It is the SAX2 counterpart of the old org.xml.sax.HandlerBase, and it differs from it exactly
// where SAX2 differs from SAX1: ContentHandler instead of DocumentHandler, which means that here
// there are startPrefixMapping/endPrefixMapping/skippedEntity which HandlerBase does not even
// know, and startElement carries (uri, localName, qName, Attributes) instead of a bare name and an
// AttributeList.
//
// Two of the default answers are decisions, not gaps:
//
//   - resolveEntity returns null, which means "no substitution, open the system id yourself".
//   - warning() and error() return quietly, so recoverable problems are accepted silently; only
//     fatalError() throws, rethrowing what it was given. A fatal error ends the analysis by
//     definition, and swallowing it would leave the caller believing the document was read.
//
// Unlike HandlerBase.resolveEntity, this one keeps IOException in its throws clause, because a
// subclass that opens a file or a URL in order to answer needs somewhere to put the failure.
//
// ContentHandler.declaration() is not overridden here: it is a default method of the interface
// whose default body already does nothing, which is the same answer this class would give.
// Overriding it with an empty body would add a member the JDK does not declare in this class.
//
// COMPILATION NOTE, and it is not cosmetic: `ContentHandler` is written with its full name in the
// `implements` clause below. The house javac, when it receives in the SAME invocation the source of
// org/xml/sax/ContentHandler.java and this file, ignores the `import org.xml.sax.ContentHandler`
// here and resolves the simple name against java.net.ContentHandler, which exists and is something
// else. The .class comes out declaring that it implements the wrong interface: it compiles,
// measures fine, and then `x instanceof ContentHandler` gives false and no parser accepts this
// class as a handler. Compiling this file alone it does not happen; the project asks for the types
// that reference each other to be compiled in one single invocation, so the way out is to qualify.
// It is finding #530 of the report (the note said #466, the number it had before the renumbering),
// with the repro and the ablation of the trigger; that it also comes out silently instead of as a
// compilation error is #467. Still reproduces as of 2026-09-18.
public class DefaultHandler
        implements EntityResolver, DTDHandler,
                   org.xml.sax.ContentHandler, ErrorHandler {

    public DefaultHandler() {
    }

    ////////////////////////////////////////////////////////////////////
    // EntityResolver
    ////////////////////////////////////////////////////////////////////

    public InputSource resolveEntity(String publicId, String systemId)
            throws IOException, SAXException {
        return null;
    }

    ////////////////////////////////////////////////////////////////////
    // DTDHandler
    ////////////////////////////////////////////////////////////////////

    public void notationDecl(String name, String publicId, String systemId)
            throws SAXException {
    }

    public void unparsedEntityDecl(String name, String publicId,
                                   String systemId, String notationName)
            throws SAXException {
    }

    ////////////////////////////////////////////////////////////////////
    // ContentHandler
    ////////////////////////////////////////////////////////////////////

    public void setDocumentLocator(Locator locator) {
    }

    public void startDocument() throws SAXException {
    }

    public void endDocument() throws SAXException {
    }

    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }

    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
            throws SAXException {
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
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

    public void skippedEntity(String name) throws SAXException {
    }

    ////////////////////////////////////////////////////////////////////
    // ErrorHandler
    ////////////////////////////////////////////////////////////////////

    public void warning(SAXParseException e) throws SAXException {
    }

    public void error(SAXParseException e) throws SAXException {
    }

    // The one that does not keep quiet, for the reason the comment of the class gives.
    public void fatalError(SAXParseException e) throws SAXException {
        throw e;
    }
}
