package org.xml.sax.ext;

import java.io.IOException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * KajiLibrary's org.xml.sax.ext.DefaultHandler2 -- `DefaultHandler` plus the three interfaces of
 * `ext`, all with an empty body.
 *
 * <p>It is the same idea as `DefaultHandler`: a handler that only wants to see the comments should
 * not have to write the other twenty-odd methods. Extending this class, one writes `comment` and
 * nothing else.
 *
 * <p>The only method that does **not** have an empty body is the two-argument `resolveEntity`, and
 * there lies what is interesting about the class. `DefaultHandler` resolved it by returning `null`
 * directly; here it is redirected to the four-argument one of {@link EntityResolver2}, passing it
 * `null` as name and baseURI. The practical consequence is that a subclass that only overrides the
 * four-argument version is also well served when the parser is old and calls the two-argument one
 * --which is exactly what one expects when overriding "the" resolver--. Without that bridge both
 * would have to be overridden and kept in step.
 *
 * <p>The same thing the other way round cannot be done: the four-argument one cannot delegate to
 * the two-argument one without throwing away the name and the base, which are precisely the data it
 * exists for.
 *
 * <p>That `getExternalSubset` and `resolveEntity` return `null` is not a stub: it is the answer the
 * contract defines for "I substitute nothing, open it by its system identifier". A default handler
 * that invented a DTD would be the one lying.
 *
 * <p>The `fatalError` of `DefaultHandler` is inherited as well, which rethrows instead of keeping
 * quiet, for the reason explained there: a fatal error ends the analysis by definition and
 * swallowing it would leave the caller believing it read the document.
 */
public class DefaultHandler2 extends org.xml.sax.helpers.DefaultHandler
        implements LexicalHandler, DeclHandler, EntityResolver2 {

    public DefaultHandler2() {
    }

    ////////////////////////////////////////////////////////////////////
    // LexicalHandler
    ////////////////////////////////////////////////////////////////////

    public void startCDATA() throws SAXException {
    }

    public void endCDATA() throws SAXException {
    }

    public void startDTD(String name, String publicId, String systemId)
            throws SAXException {
    }

    public void endDTD() throws SAXException {
    }

    public void startEntity(String name) throws SAXException {
    }

    public void endEntity(String name) throws SAXException {
    }

    public void comment(char ch[], int start, int length) throws SAXException {
    }

    ////////////////////////////////////////////////////////////////////
    // DeclHandler
    ////////////////////////////////////////////////////////////////////

    public void attributeDecl(String eName, String aName, String type,
                              String mode, String value) throws SAXException {
    }

    public void elementDecl(String name, String model) throws SAXException {
    }

    public void externalEntityDecl(String name, String publicId, String systemId)
            throws SAXException {
    }

    public void internalEntityDecl(String name, String value) throws SAXException {
    }

    ////////////////////////////////////////////////////////////////////
    // EntityResolver2
    ////////////////////////////////////////////////////////////////////

    public InputSource getExternalSubset(String name, String baseURI)
            throws SAXException, IOException {
        return null;
    }

    public InputSource resolveEntity(String name, String publicId,
                                     String baseURI, String systemId)
            throws SAXException, IOException {
        return null;
    }

    /** The bridge to the four-argument version the comment of the class explains. */
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        return resolveEntity(null, publicId, null, systemId);
    }
}
