package javax.xml.stream.events;

import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.stream.events.Attribute -- an element's attribute, seen as an event.
 *
 * <h2>An event that does not appear in the stream</h2>
 *
 * <p>It is the oddity of the model: {@code Attribute} extends {@link XMLEvent} and answers true to
 * {@link XMLEvent#isAttribute()}, but a {@link javax.xml.stream.XMLEventReader} never returns one.
 * The attributes arrive hanging from the {@link StartElement} that declares them, through {@link
 * StartElement#getAttributes()}.
 *
 * <p>That it is still an event is no whim: whoever rewrites a document needs to be able to pass an
 * attribute to {@link javax.xml.stream.XMLEventWriter#add}, and whoever filters needs to be able to
 * write it with {@link XMLEvent#writeAsEncodedUnicode}. Without the common type two paths would be
 * needed for the same thing.
 *
 * <h2>{@link #isSpecified()} and the difference with the DTD</h2>
 *
 * <p>An attribute can be in the document or come from a default value declared in the DTD. Both
 * look the same from {@link #getValue()}, and {@link #isSpecified()} is the only thing that tells
 * them apart. It matters when rewriting: an attribute the parser made up from the DTD and that is
 * written again ends up duplicated in the output document if the DTD is copied as well.
 *
 * <p>A parser that does not read the DTD --like this library's-- always returns true, which is the
 * truth: everything it saw was written.
 */
public interface Attribute extends XMLEvent {

    /**
     * The name of the attribute, with a namespace if it has one.
     *
     * <p>An attribute without a prefix is <b>not</b> in the default namespace --that is the
     * asymmetry with elements, and it comes from the Namespaces specification-- so its {@link
     * QName} has the empty namespace.
     *
     * @return the qualified name; never null
     */
    QName getName();

    /**
     * The normalized value of the attribute.
     *
     * <p>The entity references and line endings come already resolved, and --if the parser reads
     * the DTD-- the normalization corresponding to the declared type is applied.
     *
     * @return the value; never null, it can be the empty string
     */
    String getValue();

    /**
     * The type declared in the DTD: {@code CDATA}, {@code ID}, {@code IDREF}, {@code NMTOKEN}, etc.
     *
     * @return the type, or {@code "CDATA"} if there is no DTD to consult
     */
    String getDTDType();

    /**
     * Whether the attribute was written in the document, instead of coming from a default value.
     *
     * @return true if it appeared in the text
     */
    boolean isSpecified();
}
