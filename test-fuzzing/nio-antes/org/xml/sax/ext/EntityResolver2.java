package org.xml.sax.ext;

import java.io.IOException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * KajiLibrary's org.xml.sax.ext.EntityResolver2 -- the SAX1 resolver fixed in the two places where
 * it fell short.
 *
 * <p>The first: `EntityResolver.resolveEntity(publicId, systemId)` receives a system identifier the
 * parser **already resolved** against the base, so the resolver sees an absolute URI and cannot
 * know what the document said nor what it was resolved against. Here the four data arrive
 * separately --name, publicId, baseURI and the systemId as it was written-- and with that one can
 * decide.
 *
 * <p>The second, and it is the one that motivated the extension: **there was no way of giving a DTD
 * to a document that does not ask for one.** A document with no `&lt;!DOCTYPE&gt;` generates no
 * resolution call, and with no DTD there are no attribute default values nor declared entities.
 * {@link #getExternalSubset} is called for every document that has no external subset of its own,
 * precisely so that one can be injected.
 *
 * <p>It is installed in the same place as the usual one, with `setEntityResolver`; the parser does
 * `instanceof` and uses the new methods if they are there. That it uses them shows in the feature
 * `http://xml.org/sax/features/use-entity-resolver2`, which is read-write: setting it to `false`
 * asks the parser to treat the object as an old `EntityResolver` even though it implements this
 * interface.
 *
 * <p><strong>Returning `null` from either of the two methods is not a stub</strong>, it is the
 * answer the contract defines for "I have nothing to substitute, open it yourself by the system
 * identifier". It is what {@link DefaultHandler2} does.
 *
 * <p>About the order: `getExternalSubset` is called **before** `LexicalHandler.startDTD`, and
 * whatever it returns is analysed as if it were the declared external subset. If the document
 * already has one, this method is not called and there is no way of adding a second.
 */
public interface EntityResolver2 extends EntityResolver {

    /**
     * An external subset for a document that declares none, or `null` to leave it with no DTD.
     * `name` is the name of the root element, which is the only datum with which a DTD can be
     * chosen for a document that did not name one.
     */
    InputSource getExternalSubset(String name, String baseURI)
            throws SAXException, IOException;

    /**
     * `name` is `[dtd]` for the external subset, or the name of the entity --with a `%` in front if
     * it is a parameter one--. `systemId` comes **unresolved**, as it appears in the document;
     * `baseURI` is what it would have to be resolved against, and may be `null` when the parser
     * does not know where the entity that contains it came from.
     */
    InputSource resolveEntity(String name, String publicId,
                              String baseURI, String systemId)
            throws SAXException, IOException;
}
