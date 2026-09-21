package javax.xml.transform.sax;

import javax.xml.transform.Templates;
import org.xml.sax.ContentHandler;

/**
 * KajiLibrary's javax.xml.transform.sax.TemplatesHandler -- compiles a stylesheet from events.
 *
 * <p>A {@link ContentHandler} that is sent a stylesheet as SAX events and that, when it finishes,
 * hands over the compiled stylesheet in {@link #getTemplates}.
 *
 * <p>It serves the same purpose as a {@code TransformerFactory.newTemplates(Source)}, with a
 * difference that sometimes decides: the stylesheet can come from something that <b>is not a
 * file</b> -- the output of another transformation, a filter, a stream being generated-- without
 * having to write it anywhere first.
 *
 * <p>{@link #getTemplates} before the document finishes makes no sense and returns null: until
 * {@code endDocument} there is no compiled stylesheet.
 *
 * <p>The system identifier has to be set <b>before</b> starting to send events, because it is what
 * the {@code xsl:import}s and {@code xsl:include}s that appear are resolved against.
 */
public interface TemplatesHandler extends ContentHandler {

    /**
     * The compiled stylesheet.
     *
     * @return null if the document has not finished yet
     */
    Templates getTemplates();

    /** Where the stylesheet comes from. Set it before the first event; see the class note. */
    void setSystemId(String systemID);

    /** Ver {@link #setSystemId}. */
    String getSystemId();
}
