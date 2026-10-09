package org.xml.sax;

import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;

// KajiLibrary's org.xml.sax.ErrorHandler -- three severities, and the difference between them is
// not cosmetic.
//
//   warning    -- something the parser wants to mention. The analysis goes on.
//   error      -- a recoverable error, in the XML sense: a violation of *validity*. The document
//                 is invalid but is still well formed, so the parser can go on, and does, reporting
//                 events. If the application wants to stop, it throws from here.
//   fatalError -- well-formedness was lost. The parser must not report more events after this
//                 call returns, so an implementation that returns normally from fatalError is left
//                 with undefined behaviour. The honest thing is to throw.
//
// A reader with no ErrorHandler installed discards warnings and errors silently and throws on
// fatal errors --which is why installing one is the first thing every real SAX application does.
public interface ErrorHandler {

    // A non-fatal warning; the analysis goes on.
    void warning(SAXParseException exception) throws SAXException;

    // A recoverable (validity) error; the analysis goes on unless this throws.
    void error(SAXParseException exception) throws SAXException;

    // Well-formedness is broken; no more events are going to follow. It should throw.
    void fatalError(SAXParseException exception) throws SAXException;
}
