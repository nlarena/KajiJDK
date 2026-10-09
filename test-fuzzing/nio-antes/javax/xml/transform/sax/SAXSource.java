package javax.xml.transform.sax;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * KajiLibrary's javax.xml.transform.sax.SAXSource -- a source that is read as events.
 *
 * <p>It puts together two things: <b>where</b> to read from --the {@link InputSource}-- and
 * <b>who</b> reads it --the {@link XMLReader}--. The reader is optional: without one, the
 * transformer uses its own. Setting it serves to put a filter in between, or a parser configured in
 * a particular way, without the transformer finding out.
 *
 * <p>{@link #sourceToInputSource} is the interesting utility and is here for convenience: it turns
 * any {@code Source} into an {@code InputSource} when it can. It can with this one and with {@link
 * StreamSource}; with the others it returns <b>null</b> instead of throwing, because a {@code
 * DOMSource} has nothing to read as events and that is not an error but an answer.
 */
public class SAXSource implements Source {

    /** With this a {@code TransformerFactory} is asked whether it accepts this source. */
    public static final String FEATURE = "http://javax.xml.transform.sax.SAXSource/feature";

    private XMLReader reader;

    private InputSource inputSource;

    /** Empty, to be filled. */
    public SAXSource() {
    }

    /** With a reader of its own. */
    public SAXSource(XMLReader reader, InputSource inputSource) {
        this.reader = reader;
        this.inputSource = inputSource;
    }

    /** Without a reader: the transformer supplies it. */
    public SAXSource(InputSource inputSource) {
        this.inputSource = inputSource;
    }

    /** The reader, or null for the transformer to choose it. */
    public void setXMLReader(XMLReader reader) {
        this.reader = reader;
    }

    /** Ver {@link #setXMLReader}. */
    public XMLReader getXMLReader() {
        return this.reader;
    }

    /** Where to read from. */
    public void setInputSource(InputSource inputSource) {
        this.inputSource = inputSource;
    }

    /** Ver {@link #setInputSource}. */
    public InputSource getInputSource() {
        return this.inputSource;
    }

    /**
     * Where it came from.
     *
     * <p>If there is no {@link InputSource} yet, it <b>creates</b> one with that identifier. It is
     * what makes setting only the identifier enough to have a usable source.
     */
    public void setSystemId(String systemId) {
        if (this.inputSource == null) {
            this.inputSource = new InputSource(systemId);
        } else {
            this.inputSource.setSystemId(systemId);
        }
    }

    /** The {@link InputSource}'s, or null if there is none. */
    public String getSystemId() {
        if (this.inputSource == null) {
            return null;
        }
        return this.inputSource.getSystemId();
    }

    /**
     * An equivalent {@code InputSource}, if possible.
     *
     * @return null if that source cannot be read as events; see the class note
     */
    public static InputSource sourceToInputSource(Source source) {
        if (source instanceof SAXSource) {
            return ((SAXSource) source).getInputSource();
        }
        if (source instanceof StreamSource) {
            StreamSource stream = (StreamSource) source;
            InputSource made = new InputSource(stream.getSystemId());
            made.setByteStream(stream.getInputStream());
            made.setCharacterStream(stream.getReader());
            made.setPublicId(stream.getPublicId());
            return made;
        }
        return null;
    }

    /**
     * Whether there is nothing to read.
     *
     * <p>Having an {@link InputSource} is not enough: a newly built one is as empty as none. What
     * counts is that it has one of the three routes --identifier, byte stream or character
     * stream--.
     */
    public boolean isEmpty() {
        InputSource where = getInputSource();
        if (where == null) {
            return true;
        }
        return where.getSystemId() == null
            && where.getByteStream() == null
            && where.getCharacterStream() == null;
    }
}
