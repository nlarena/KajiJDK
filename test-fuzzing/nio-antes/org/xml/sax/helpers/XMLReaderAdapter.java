package org.xml.sax.helpers;

import java.io.IOException;
import java.util.Locale;

import org.xml.sax.AttributeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

// KajiLibrary's org.xml.sax.helpers.XMLReaderAdapter -- a SAX2 reader with a SAX1 face.
//
// It is the mirror of ParserAdapter: that one makes an old parser usable from new code, this one
// makes a new parser usable from old code. It implements Parser (SAX1) on the outside and
// ContentHandler (SAX2) on the inside, so it can register itself with the XMLReader it wraps and
// translate each event on the way through.
//
// It analyses nothing. Everything downstream of parse() is the job of the wrapped XMLReader; this
// class only builds a bridge between two forms of the same event.
//
// The translation is almost all erasure, because SAX1 knows less:
//
//   - startElement(uri, localName, qName, Attributes) becomes startElement(qName, AttributeList):
//     the namespace URI and the local name are discarded and what SAX1 sees is the qualified name,
//     that is the name as it is written in the document.
//   - startPrefixMapping/endPrefixMapping are discarded entirely: SAX1 does not have those events,
//     and the information they carry survives only as xmlns attributes.
//   - skippedEntity is discarded for the same reason.
//
// For that to work the adapter reconfigures the reader in setupXMLReader(), and the two features it
// touches are not optional for fun:
//
//   namespace-prefixes = true    keep the xmlns attributes in the attribute list, since a SAX1
//                                handler has no other way of finding out about namespaces. This one
//                                is mandatory; if the reader refuses, the analysis fails.
//   namespaces         = false   do not spend time computing URIs nobody downstream is going to
//                                see. This one is an optimisation, so a refusal is swallowed.
//
// setLocale() always throws SAXNotSupportedException: SAX2 dropped locale negotiation, so there is
// nobody to forward it to. Answering otherwise would be asserting something the wrapped reader
// cannot fulfil.
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
public class XMLReaderAdapter
        implements Parser, org.xml.sax.ContentHandler {

    XMLReader xmlReader;
    DocumentHandler documentHandler;
    AttributesAdapter qAtts;

    // It wraps whatever XMLReaderFactory finds. In KajiLibrary that means the driver the system
    // property `org.xml.sax.driver` names, since no parser comes included; with nothing configured,
    // the SAXException comes out of the factory and is passed straight through.
    public XMLReaderAdapter() throws SAXException {
        setup(XMLReaderFactory.createXMLReader());
    }

    public XMLReaderAdapter(XMLReader xmlReader) {
        setup(xmlReader);
    }

    private void setup(XMLReader xmlReader) {
        if (xmlReader == null) {
            throw new NullPointerException("XMLReader must not be null");
        }
        this.xmlReader = xmlReader;
        qAtts = new AttributesAdapter();
    }

    ////////////////////////////////////////////////////////////////////
    // Parser (SAX1), the face towards the outside
    ////////////////////////////////////////////////////////////////////

    // There is no equivalent in SAX2, so the only honest thing is to refuse.
    public void setLocale(Locale locale) throws SAXException {
        throw new SAXNotSupportedException("setLocale not supported");
    }

    // These three go straight through: the interfaces do not change between SAX1 and SAX2.
    public void setEntityResolver(EntityResolver resolver) {
        xmlReader.setEntityResolver(resolver);
    }

    public void setDTDHandler(DTDHandler handler) {
        xmlReader.setDTDHandler(handler);
    }

    public void setErrorHandler(ErrorHandler handler) {
        xmlReader.setErrorHandler(handler);
    }

    // This one does not: the content handler of the reader is *this* adapter, and the SAX1 handler
    // is kept here to forward to.
    public void setDocumentHandler(DocumentHandler handler) {
        documentHandler = handler;
    }

    public void parse(String systemId) throws IOException, SAXException {
        parse(new InputSource(systemId));
    }

    public void parse(InputSource input) throws IOException, SAXException {
        setupXMLReader();
        xmlReader.parse(input);
    }

    // The negotiation of features described in the comment of the class.
    private void setupXMLReader() throws SAXException {
        // Mandatory: without the xmlns attributes a SAX1 handler is blind to namespaces.
        xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes",
                             true);
        try {
            xmlReader.setFeature("http://xml.org/sax/features/namespaces",
                                 false);
        } catch (SAXException e) {
            // Optional: this only saves the reader work, and a reader that insists on processing
            // namespaces still produces the events needed.
        }
        xmlReader.setContentHandler(this);
    }

    ////////////////////////////////////////////////////////////////////
    // ContentHandler (SAX2), the face towards the inside
    ////////////////////////////////////////////////////////////////////

    public void setDocumentLocator(Locator locator) {
        if (documentHandler != null) {
            documentHandler.setDocumentLocator(locator);
        }
    }

    public void startDocument() throws SAXException {
        if (documentHandler != null) {
            documentHandler.startDocument();
        }
    }

    public void endDocument() throws SAXException {
        if (documentHandler != null) {
            documentHandler.endDocument();
        }
    }

    // Discarded: SAX1 has no prefix mapping events. Declared without throwing anything, unlike the
    // interface, because here there is nothing that can fail.
    public void startPrefixMapping(String prefix, String uri) {
    }

    public void endPrefixMapping(String prefix) {
    }

    // The only real translation: three parts of the name become one, and the SAX2 Attributes is
    // wrapped instead of copied. The wrapper is reused between elements, so a SAX1 handler that
    // keeps the AttributeList beyond the end of startElement is looking at the attributes of the
    // next element: the usual rule in SAX, and the reason why AttributeListImpl has a copy
    // constructor.
    public void startElement(String uri, String localName,
                             String qName, Attributes atts)
            throws SAXException {
        if (documentHandler != null) {
            qAtts.setAttributes(atts);
            documentHandler.startElement(qName, qAtts);
        }
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (documentHandler != null) {
            documentHandler.endElement(qName);
        }
    }

    public void characters(char ch[], int start, int length)
            throws SAXException {
        if (documentHandler != null) {
            documentHandler.characters(ch, start, length);
        }
    }

    public void ignorableWhitespace(char ch[], int start, int length)
            throws SAXException {
        if (documentHandler != null) {
            documentHandler.ignorableWhitespace(ch, start, length);
        }
    }

    public void processingInstruction(String target, String data)
            throws SAXException {
        if (documentHandler != null) {
            documentHandler.processingInstruction(target, data);
        }
    }

    // Discarded: SAX1 has no notion of a skipped entity.
    public void skippedEntity(String name) throws SAXException {
    }

    ////////////////////////////////////////////////////////////////////

    // A SAX2 Attributes seen through the SAX1 AttributeList keyhole: the names are qualified names,
    // and the two lookups that know about namespaces simply do not exist to be asked.
    final class AttributesAdapter implements AttributeList {

        private Attributes atts;

        AttributesAdapter() {
        }

        void setAttributes(Attributes atts) {
            this.atts = atts;
        }

        public int getLength() {
            return atts.getLength();
        }

        // The qualified name, which is what SAX1 understands by "the name".
        public String getName(int i) {
            return atts.getQName(i);
        }

        public String getType(int i) {
            return atts.getType(i);
        }

        public String getValue(int i) {
            return atts.getValue(i);
        }

        public String getType(String qName) {
            return atts.getType(qName);
        }

        public String getValue(String qName) {
            return atts.getValue(qName);
        }
    }
}
