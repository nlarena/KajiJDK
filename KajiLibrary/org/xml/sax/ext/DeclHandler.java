package org.xml.sax.ext;

import org.xml.sax.SAXException;

/**
 * KajiLibrary's org.xml.sax.ext.DeclHandler -- what the DTD *declares*, not what the document
 * says.
 *
 * <p>The `DTDHandler` of the core reports two things and nothing else: notations and unparsed
 * entities. That is enough for resolving references to binary data and for nothing else. Whoever
 * wants to know the content model of an element, or the default value of an attribute, or the text
 * of an internal entity --that is, whoever wants to **copy the DTD**-- needs this interface.
 *
 * <p>It is installed with the `http://xml.org/sax/properties/declaration-handler` property of the
 * `XMLReader`, and like every extension the parser may not recognise it.
 *
 * <p>The events fall between `startDTD` and `endDTD` of the {@link LexicalHandler}, when one is
 * installed. A **non-validating** parser may skip the whole external subset, and then nothing comes
 * out of here even though the DTD exists: it is not a breach, it is that reading the external DTD
 * is optional. The way of knowing is the feature
 * `http://xml.org/sax/features/external-parameter-entities`.
 *
 * <p>About what is **not** here: the unparsed entities and the notations still go through
 * `org.xml.sax.DTDHandler`. They have been split like that since SAX1 and duplicating them here
 * would be inventing members the contract does not have.
 *
 * <p><strong>In KajiLibrary nobody produces these events yet</strong>: the tree brings no XML
 * parser, so the interface is complete but has no emitter of its own.
 */
public interface DeclHandler {

    /**
     * The content model arrives **as text**, already normalised to the form of the standard:
     * `EMPTY`, `ANY`, or an expression with parentheses such as `(#PCDATA|a|b)*`. It does not come
     * parsed, and that is on purpose: parsing it is the job of whoever needs it, and returning it
     * raw loses no information.
     */
    void elementDecl(String name, String model) throws SAXException;

    /**
     * `type` is the declared type --`CDATA`, `ID`, a `NOTATION (a|b)` list or an `(a|b)`
     * enumeration--. `valueDefault` is `#IMPLIED`, `#REQUIRED`, `#FIXED` or `null` when there is an
     * ordinary default value; `value` is that value, or `null` if there is none. The last two are
     * read together: `#FIXED` with `value` is a fixed value, `null` with `value` is an ordinary
     * default value.
     */
    void attributeDecl(String eName, String aName, String type,
                       String valueDefault, String value) throws SAXException;

    /**
     * The value comes with the character references and the parameter entity references already
     * expanded, but **without** expanding the general entity references: expanding them here would
     * give the final text instead of the declaration, which is precisely what is being reported.
     */
    void internalEntityDecl(String name, String value) throws SAXException;

    /** Parameter entities arrive with a `%` in front, just as in `startEntity`. */
    void externalEntityDecl(String name, String publicId, String systemId) throws SAXException;
}
