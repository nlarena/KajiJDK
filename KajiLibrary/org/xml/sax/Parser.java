package org.xml.sax;

import java.io.IOException;
import java.util.Locale;
import org.xml.sax.DTDHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

// KajiLibrary's org.xml.sax.Parser -- the SAX1 reader interface, replaced by XMLReader.
//
// Two things set it apart from XMLReader and both explain why it was replaced: there are no
// namespaces (it feeds a DocumentHandler, not a ContentHandler) and there is no mechanism of
// features and properties --setLocale is the only knob, and that is why every SAX1 parser ended
// up with its own incompatible configuration methods.
//
// helpers.XMLReaderAdapter implements this interface over a SAX2 XMLReader;
// helpers.ParserAdapter implements XMLReader over one of these. Deprecated in the JDK; see
// AttributeList for why @Deprecated is not written here.
public interface Parser {

    // It asks for the error messages in a locale. It throws SAXException if the locale is not
    // supported --the only point of configuration SAX1 managed to standardise.
    void setLocale(Locale locale) throws SAXException;

    // It installs the resolver of external entities; null means the default behaviour.
    void setEntityResolver(EntityResolver resolver);

    // It installs the handler of the DTD.
    void setDTDHandler(DTDHandler handler);

    // It installs the handler of the document. Without one, the parser reports nothing.
    void setDocumentHandler(DocumentHandler handler);

    // It installs the error handler. Without one, errors are silent and fatal ones throw.
    void setErrorHandler(ErrorHandler handler);

    // It analyses a document. It is not reentrant: one analysis at a time per Parser instance.
    void parse(InputSource source) throws SAXException, IOException;

    // A shortcut for parse(new InputSource(systemId)).
    void parse(String systemId) throws SAXException, IOException;
}
