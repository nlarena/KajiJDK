package org.xml.sax;

import java.io.IOException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

// KajiLibrary's org.xml.sax.EntityResolver -- the hook that lets an application intercept each
// external entity the parser is about to fetch (the external subset of the DTD, the external
// parsed entities) and put its own bytes in its place.
//
// It is the interface behind two very different practices. The good one: redirecting a known
// public identifier to a local copy of the catalogue, so that a build does not depend on a remote
// DTD being up. The defensive one: returning an empty InputSource to deny the fetch outright,
// which is the classic mitigation of XXE --an untrusted document that names a system entity it
// should not be able to read.
//
// Returning null means "resolve it as usual", that is, open the system identifier yourself.
public interface EntityResolver {

    // It returns the InputSource the parser should read in its place, or null to let it open the
    // system identifier on its own.
    InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException;
}
