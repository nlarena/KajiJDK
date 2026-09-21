package org.xml.sax.ext;

import org.xml.sax.Attributes;

/**
 * KajiLibrary's org.xml.sax.ext.Attributes2 -- `Attributes` plus the two questions the flat list
 * cannot answer: was this attribute in the DTD? was it written on the element, or did the parser
 * put it there?
 *
 * <p>The second matters more than it seems. An attribute with a default value in the DTD appears in
 * `startElement` exactly the same as one written by hand, and for reading the document that is fine
 * --the effective value is the same--. But a serialiser that rewrites the document and emits both
 * ends up putting into the file things the author did not write, and also breaks it if it is later
 * read without the DTD. `isSpecified` is the only way of telling them apart.
 *
 * <p>The handler receives this without asking for it: the parser passes an `Attributes` to
 * `startElement` and the code does `instanceof Attributes2` to see whether it has the extra
 * information. There is no feature that switches it on. The one that does tell is
 * `http://xml.org/sax/features/use-attributes2`, which a parser reports as `true` (read-only) when
 * it hands over objects of this type.
 *
 * <p><strong>The asymmetry with `Attributes` belongs to the contract and is not an
 * oversight:</strong> there a name that does not exist returns `null`, here it throws. The reason
 * is the return type: with a `boolean` there is no third value to say "there is no such attribute",
 * and returning `false` would be lying --it would be asserting that it exists and was not
 * specified--. An index out of range gives `ArrayIndexOutOfBoundsException`; a name that is not
 * there, `IllegalArgumentException`.
 *
 * <p>When there is no DTD, `isDeclared` is `false` for everything and `isSpecified` is `true` for
 * everything, which is the right answer and not a filler value: with no DTD nothing is declared and
 * everything there is was written.
 */
public interface Attributes2 extends Attributes {

    /**
     * `true` if the attribute was declared in the DTD. A non-validating parser that skips the
     * external subset is going to say `false` of attributes that were declared over there: it is
     * answering for what it read, not for what the document has.
     */
    boolean isDeclared(int index);

    boolean isDeclared(String qName);

    boolean isDeclared(String uri, String localName);

    /**
     * `false` only when the value came from a `#FIXED` or from a default value of the DTD. An
     * attribute written on the element gives `true` even if it matches the default value.
     */
    boolean isSpecified(int index);

    boolean isSpecified(String uri, String localName);

    boolean isSpecified(String qName);
}
