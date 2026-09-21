package org.xml.sax.ext;

import org.xml.sax.Locator;

/**
 * KajiLibrary's org.xml.sax.ext.Locator2 -- the `Locator` plus the version and the encoding of the
 * entity the parser is standing in now.
 *
 * <p>It is not the same as "the version and the encoding of the document", and there lies the
 * detail: an XML 1.0 document may include external entities with their own declaration, and while
 * one of them is being read these two methods answer for **that** entity. As the `Locator` is alive
 * --it keeps answering for the current position as the analysis advances--, the answers change
 * during the walk. Whoever wants the version of the document has to read it early, or keep it via
 * `ContentHandler.declaration`.
 *
 * <p>It is discovered like {@link Attributes2}: the object that arrives at `setDocumentLocator` may
 * be one of these, and the code finds out with `instanceof`. There is no feature that switches it
 * on.
 *
 * <p>Both answers may be `null`, and they mean different things depending on the method: in
 * `getEncoding` it is "not known" --it happens when the encoding came from outside, for example
 * from an HTTP header, and the parser did not propagate it--; in `getXMLVersion` it should not
 * happen during the analysis, because an entity with no declaration is 1.0 by definition.
 */
public interface Locator2 extends Locator {

    /**
     * `"1.0"` or `"1.1"`. It is that of the current entity, not necessarily that of the document.
     */
    String getXMLVersion();

    /**
     * The name of the encoding in use. If the document declared it, it is what was declared; if it
     * was deduced from the BOM or from the first bytes, it is what was deduced; if it came from an
     * `InputSource` with a ready-made `Reader`, the parser does not know it and returns `null`.
     */
    String getEncoding();
}
