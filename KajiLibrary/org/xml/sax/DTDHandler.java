package org.xml.sax;

import org.xml.sax.SAXException;

// KajiLibrary's org.xml.sax.DTDHandler -- the two declarations of the DTD SAX reports to
// *every* application, whether it validates or not.
//
// Why only these two: an unparsed entity is content that is not XML (an image, for example) that
// the document refers to by name, and a notation says what kind of thing it is. An application
// that wants to follow that reference has no other way of resolving the name, so SAX considers
// these two declarations part of the basic contract and leaves the rest of the DTD (elements,
// attributes, parsed entities) to the optional ext.DeclHandler.
//
// The two events are reported before the element of the document starts.
public interface DTDHandler {

    // A notation declaration. Exactly one of publicId/systemId may be null.
    void notationDecl(String name, String publicId, String systemId) throws SAXException;

    // An unparsed entity declaration. `notationName` names a notation declared elsewhere in the
    // DTD, and publicId may be null.
    void unparsedEntityDecl(String name, String publicId, String systemId, String notationName)
            throws SAXException;
}
