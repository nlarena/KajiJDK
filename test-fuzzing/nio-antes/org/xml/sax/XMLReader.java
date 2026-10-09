package org.xml.sax;

import java.io.IOException;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

// KajiLibrary's org.xml.sax.XMLReader -- the SAX2 reader: the object an application configures
// and then sends to walk a document.
//
// What it adds over the SAX1 Parser is the feature/property pair. Both are indexed by a string
// with a URI, which was how SAX avoided standardising one method per knob and still let vendors
// extend:
//
//   * a *feature* is a boolean switch. The two that matter are
//     http://xml.org/sax/features/namespaces (on by default: it reports uri/localName) and
//     .../namespace-prefixes (off by default: it also reports the qNames and the xmlns
//     attributes). At least one of the two has to be on.
//   * a *property* is any object, for example .../properties/lexical-handler.
//
// The two exceptions are the whole protocol: SAXNotRecognizedException for a name the reader
// never heard of, SAXNotSupportedException for one it knows but cannot attend to now --reading a
// write-only property, or changing a feature in the middle of the analysis.
//
// parse() is synchronous and not reentrant; the reader can be reused for another document once it
// returns.
public interface XMLReader {

    // It queries the value of a feature.
    boolean getFeature(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException;

    // It changes a feature. Some are read-only during an analysis.
    void setFeature(String name, boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException;

    // It queries the value of a property.
    Object getProperty(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException;

    // It changes a property.
    void setProperty(String name, Object value)
            throws SAXNotRecognizedException, SAXNotSupportedException;

    // It installs the resolver of external entities; null goes back to the default behaviour.
    void setEntityResolver(EntityResolver resolver);

    // The current entity resolver, or null.
    EntityResolver getEntityResolver();

    // It installs the handler of the DTD; null goes back to the default one.
    void setDTDHandler(DTDHandler handler);

    // The current DTD handler, or null.
    DTDHandler getDTDHandler();

    // It installs the content handler --the one that really receives the document.
    void setContentHandler(ContentHandler handler);

    // The current content handler, or null.
    ContentHandler getContentHandler();

    // It installs the error handler; without one, warnings and recoverable errors are
    // discarded.
    void setErrorHandler(ErrorHandler handler);

    // The current error handler, or null.
    ErrorHandler getErrorHandler();

    // It analyses a document, blocking until it finishes. One at a time per reader.
    void parse(InputSource input) throws IOException, SAXException;

    // A shortcut for parse(new InputSource(systemId)).
    void parse(String systemId) throws IOException, SAXException;
}
