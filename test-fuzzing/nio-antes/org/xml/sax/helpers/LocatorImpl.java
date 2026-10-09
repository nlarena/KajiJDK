package org.xml.sax.helpers;

import org.xml.sax.Locator;

// KajiLibrary's org.xml.sax.helpers.LocatorImpl -- a Locator that can be kept.
//
// The Locator the parser hands over in setDocumentLocator() is alive: it answers where the
// analysis is at this moment, and asking it once the event has passed gives an answer about
// somewhere else. So a handler that wants to remember where an event happened cannot keep the
// Locator; it has to copy it. Precisely for that the copy constructor is here:
//
//     locatorForThisEvent = new LocatorImpl(theParsersLocator);
//
// Everything else is a mutable bag of four fields, which is also the reason why parsers use it to
// begin with as the Locator they hand out.
public class LocatorImpl implements Locator {

    private String publicId;
    private String systemId;
    private int lineNumber;
    private int columnNumber;

    // The four fields unassigned: ids at null and positions at zero.
    public LocatorImpl() {
    }

    // The copy constructor described above.
    public LocatorImpl(Locator locator) {
        setPublicId(locator.getPublicId());
        setSystemId(locator.getSystemId());
        setLineNumber(locator.getLineNumber());
        setColumnNumber(locator.getColumnNumber());
    }

    public String getPublicId() {
        return publicId;
    }

    public String getSystemId() {
        return systemId;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public void setColumnNumber(int columnNumber) {
        this.columnNumber = columnNumber;
    }
}
