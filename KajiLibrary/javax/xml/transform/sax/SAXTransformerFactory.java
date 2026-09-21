package javax.xml.transform.sax;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import org.xml.sax.XMLFilter;

/**
 * KajiLibrary's javax.xml.transform.sax.SAXTransformerFactory -- the factory of the SAX pieces.
 *
 * <p>It extends {@link TransformerFactory} with what is needed to transform <b>by events</b>: the
 * {@link TransformerHandler}s, the {@link TemplatesHandler}s and the filters.
 *
 * <h2>How it is obtained</h2>
 *
 * <p>It has no {@code newInstance} of its own. An ordinary {@code TransformerFactory} is asked for,
 * it is asked about {@link #FEATURE} and, if it says yes, it is converted with a cast. It is
 * awkward and it is deliberate: this class came later, and adding a {@code newInstance} to the
 * hierarchy would have forced every existing implementation to support the SAX part.
 *
 * <p>{@link #FEATURE_XMLFILTER} is a second question, for the two {@code newXMLFilter}s: an
 * implementation can give the handlers and not the filters.
 *
 * <h2>The two forms of each thing</h2>
 *
 * <p>Almost everything comes in pairs, with {@link Source} and with {@link Templates}, and the
 * difference is one of cost. With {@code Source} the stylesheet is compiled <b>on every call</b>;
 * with {@code Templates} it is already compiled and reused. For a stylesheet applied many times,
 * that is all the performance difference there is to gain.
 *
 * <p>{@link #newTransformerHandler()} without arguments gives one that transforms nothing: it
 * copies the input to the output. It sounds useless and is not -- it is the way to turn a chain of
 * events into a document, or to serialize it, using only the transformer's output properties.
 */
public abstract class SAXTransformerFactory extends TransformerFactory {

    /** With this one asks whether a factory is one of these. See the class note. */
    public static final String FEATURE =
        "http://javax.xml.transform.sax.SAXTransformerFactory/feature";

    /** And with this, whether it also knows how to make filters. */
    public static final String FEATURE_XMLFILTER =
        "http://javax.xml.transform.sax.SAXTransformerFactory/feature/xmlfilter";

    /** For the subclasses. */
    protected SAXTransformerFactory() {
    }

    /**
     * A handler that applies that stylesheet.
     *
     * <p>It compiles it on this call; see the class note on the cost.
     *
     * @throws TransformerConfigurationException if the stylesheet is wrong
     */
    public abstract TransformerHandler newTransformerHandler(Source src)
        throws TransformerConfigurationException;

    /** Likewise, with the stylesheet already compiled. */
    public abstract TransformerHandler newTransformerHandler(Templates templates)
        throws TransformerConfigurationException;

    /** One that copies the input to the output. See the class note on what it is for. */
    public abstract TransformerHandler newTransformerHandler()
        throws TransformerConfigurationException;

    /** A handler that compiles a stylesheet arriving as events. */
    public abstract TemplatesHandler newTemplatesHandler()
        throws TransformerConfigurationException;

    /**
     * A SAX filter that applies that stylesheet.
     *
     * <p>An {@link XMLFilter} is chained with {@code setParent}, so this lets a transformation be
     * put inside an already built reading chain, without touching the rest.
     */
    public abstract XMLFilter newXMLFilter(Source src) throws TransformerConfigurationException;

    /** Likewise, with the stylesheet already compiled. */
    public abstract XMLFilter newXMLFilter(Templates templates)
        throws TransformerConfigurationException;
}
