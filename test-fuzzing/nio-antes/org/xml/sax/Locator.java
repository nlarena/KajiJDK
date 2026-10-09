package org.xml.sax;

// KajiLibrary's org.xml.sax.Locator -- "what part of the document are we in now?".
//
// The parser hands one of these to the application only once, through
// ContentHandler.setDocumentLocator, *before* startDocument. The object is alive: the same
// instance goes on answering with the current position as the analysis advances. That is why the
// contract says it is only valid inside the call of an event --keeping it and consulting it later
// returns whatever the parser was doing at that moment, or rubbish. An application that wants to
// remember a position copies it into a LocatorImpl snapshot.
//
// The line and the column start at 1, and either of the two may be -1 when the parser does not
// know.
public interface Locator {

    // The public identifier of the current event of the document, or null if there is none.
    String getPublicId();

    // The system identifier (typically a URI) of the current event of the document, or null.
    String getSystemId();

    // The line number where the current event of the document ends, or -1 if it is not known. It
    // points at the *end* of the construct that produced the event, not at its start.
    int getLineNumber();

    // The column number where the current event of the document ends, or -1 if it is not known. The
    // first column is 1.
    int getColumnNumber();
}
