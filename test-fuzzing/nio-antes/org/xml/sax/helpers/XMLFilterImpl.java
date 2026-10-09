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
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

// KajiLibrary's org.xml.sax.helpers.XMLFilterImpl -- a link in a chain of SAX readers.
//
// It is an XMLReader that reads nothing: it has a *parent* XMLReader that does read, and it stands
// between that parent and the application. Downwards it looks like a handler (the parent sends it
// events); upwards it looks like a reader (the application registers handlers with it and calls
// parse on it). Everything it receives it passes on, unchanged, and everything it is asked it asks
// the parent.
//
// On its own that does nothing, and that is precisely the idea: it is a base class. A subclass
// overrides the two or three events it cares about, calls super for the rest, and has a filter
// running. The canonical form is
//
//     public void startElement(String uri, String ln, String qn, Attributes a)
//             throws SAXException {
//         super.startElement(uri, ln, qn, rewrite(a));   // forwarding is not optional
//     }
//
// and the classic bug is forgetting the call to super, which silently erases the event from the
// stream. Each of the seventeen handler methods below forwards; a filter that eats one eats it for
// everything downstream.
//
// The wiring happens in setupParse(), which is called at the start of both overloads of parse():
// the filter registers *itself* on the parent as entity resolver, DTD handler, content handler and
// error handler, overwriting whatever was there. So a handler set directly on the parent is lost
// the moment the filter analyses; the handlers go on the filter.
//
// Note the two-level storage of handlers this produces. setContentHandler() on the filter records
// the handler *of the application*, to forward to; the content handler of the parent is the filter
// itself. The getters answer with the recorded application handler, not with whatever the parent
// has at this moment.
//
// A null handler is not an error at any moment: each forwarding method checks and does nothing
// when nobody is listening. That is what makes a filter usable before it is fully wired.
//
// The feature and property calls go straight to the parent and throw SAXNotRecognizedException
// when there is no parent, because with nobody to ask it cannot be asserted that any feature is
// recognised.
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
public class XMLFilterImpl
        implements XMLFilter, EntityResolver, DTDHandler,
                   org.xml.sax.ContentHandler, ErrorHandler {

    private XMLReader parent = null;
    private Locator locator = null;
    private EntityResolver entityResolver = null;
    private DTDHandler dtdHandler = null;
    private org.xml.sax.ContentHandler contentHandler = null;
    private ErrorHandler errorHandler = null;

    // A filter with no parent yet; one has to be set before analysing.
    public XMLFilterImpl() {
        super();
    }

    public XMLFilterImpl(XMLReader parent) {
        super();
        setParent(parent);
    }

    ////////////////////////////////////////////////////////////////////
    // XMLFilter
    ////////////////////////////////////////////////////////////////////

    public void setParent(XMLReader parent) {
        this.parent = parent;
    }

    public XMLReader getParent() {
        return parent;
    }

    ////////////////////////////////////////////////////////////////////
    // XMLReader: configuration, all of it delegated
    ////////////////////////////////////////////////////////////////////

    public void setFeature(String name, boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if (parent != null) {
            parent.setFeature(name, value);
        } else {
            throw new SAXNotRecognizedException("Feature: " + name);
        }
    }

    public boolean getFeature(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if (parent != null) {
            return parent.getFeature(name);
        } else {
            throw new SAXNotRecognizedException("Feature: " + name);
        }
    }

    public void setProperty(String name, Object value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if (parent != null) {
            parent.setProperty(name, value);
        } else {
            throw new SAXNotRecognizedException("Property: " + name);
        }
    }

    public Object getProperty(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if (parent != null) {
            return parent.getProperty(name);
        } else {
            throw new SAXNotRecognizedException("Property: " + name);
        }
    }

    // These four record the handlers of the application. On purpose they are *not* forwarded to the
    // parent: setupParse() sets the parent's handlers to `this`.
    public void setEntityResolver(EntityResolver resolver) {
        entityResolver = resolver;
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public void setDTDHandler(DTDHandler handler) {
        dtdHandler = handler;
    }

    public DTDHandler getDTDHandler() {
        return dtdHandler;
    }

    public void setContentHandler(org.xml.sax.ContentHandler handler) {
        contentHandler = handler;
    }

    public org.xml.sax.ContentHandler getContentHandler() {
        return contentHandler;
    }

    public void setErrorHandler(ErrorHandler handler) {
        errorHandler = handler;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    // Analysing is the parent's job; all this does is get in the way first. A null parent gives a
    // NullPointerException, which is honest: there is nothing to analyse with.
    public void parse(InputSource input) throws SAXException, IOException {
        setupParse();
        parent.parse(input);
    }

    public void parse(String systemId) throws SAXException, IOException {
        parse(new InputSource(systemId));
    }

    ////////////////////////////////////////////////////////////////////
    // EntityResolver
    ////////////////////////////////////////////////////////////////////

    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        if (entityResolver != null) {
            return entityResolver.resolveEntity(publicId, systemId);
        } else {
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////
    // DTDHandler
    ////////////////////////////////////////////////////////////////////

    public void notationDecl(String name, String publicId, String systemId)
            throws SAXException {
        if (dtdHandler != null) {
            dtdHandler.notationDecl(name, publicId, systemId);
        }
    }

    public void unparsedEntityDecl(String name, String publicId,
                                   String systemId, String notationName)
            throws SAXException {
        if (dtdHandler != null) {
            dtdHandler.unparsedEntityDecl(name, publicId, systemId,
                                          notationName);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // ContentHandler: the eleven one must not forget
    ////////////////////////////////////////////////////////////////////

    // It is also kept here, so that a subclass can ask where it is without intercepting the event.
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
        if (contentHandler != null) {
            contentHandler.setDocumentLocator(locator);
        }
    }

    public void startDocument() throws SAXException {
        if (contentHandler != null) {
            contentHandler.startDocument();
        }
    }

    public void endDocument() throws SAXException {
        if (contentHandler != null) {
            contentHandler.endDocument();
        }
    }

    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        if (contentHandler != null) {
            contentHandler.startPrefixMapping(prefix, uri);
        }
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        if (contentHandler != null) {
            contentHandler.endPrefixMapping(prefix);
        }
    }

    public void startElement(String uri, String localName, String qName,
                             Attributes atts) throws SAXException {
        if (contentHandler != null) {
            contentHandler.startElement(uri, localName, qName, atts);
        }
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (contentHandler != null) {
            contentHandler.endElement(uri, localName, qName);
        }
    }

    public void characters(char ch[], int start, int length)
            throws SAXException {
        if (contentHandler != null) {
            contentHandler.characters(ch, start, length);
        }
    }

    public void ignorableWhitespace(char ch[], int start, int length)
            throws SAXException {
        if (contentHandler != null) {
            contentHandler.ignorableWhitespace(ch, start, length);
        }
    }

    public void processingInstruction(String target, String data)
            throws SAXException {
        if (contentHandler != null) {
            contentHandler.processingInstruction(target, data);
        }
    }

    public void skippedEntity(String name) throws SAXException {
        if (contentHandler != null) {
            contentHandler.skippedEntity(name);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // ErrorHandler
    ////////////////////////////////////////////////////////////////////

    public void warning(SAXParseException e) throws SAXException {
        if (errorHandler != null) {
            errorHandler.warning(e);
        }
    }

    public void error(SAXParseException e) throws SAXException {
        if (errorHandler != null) {
            errorHandler.error(e);
        }
    }

    // Note that with no error handler this returns silently even on a fatal error: an XMLFilterImpl
    // is a conduit, not a policy. The rethrowing lives in DefaultHandler.
    public void fatalError(SAXParseException e) throws SAXException {
        if (errorHandler != null) {
            errorHandler.fatalError(e);
        }
    }

    ////////////////////////////////////////////////////////////////////

    // It puts this filter between the parent and the application, replacing whatever the parent had
    // registered. See the comment of the class: the handlers set directly on the parent do not
    // survive this.
    private void setupParse() {
        parent.setEntityResolver(this);
        parent.setDTDHandler(this);
        parent.setContentHandler(this);
        parent.setErrorHandler(this);
    }
}
