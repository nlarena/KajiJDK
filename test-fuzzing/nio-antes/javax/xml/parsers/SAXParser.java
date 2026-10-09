package javax.xml.parsers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.validation.Schema;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * KajiLibrary's javax.xml.parsers.SAXParser -- reads XML notifying by event.
 *
 * <p>The counterpart of {@link DocumentBuilder}: it builds nothing in memory, it keeps calling a
 * handler as it reads. That is why it can read a file larger than the available memory, and why
 * whoever uses it has to keep what interests them as it goes by -- what is not kept is lost.
 *
 * <h2>The two {@code parse} families</h2>
 *
 * <p>The ones that receive a {@link HandlerBase} are SAX 1's and are deprecated; the ones that
 * receive a {@link DefaultHandler} are SAX 2's and are the ones to use. It is not just a change of
 * name: in SAX 1 elements have no namespace, so a document with prefixes arrives with the prefixes
 * stuck to the name and they have to be split by hand. Both families remain because {@code
 * javax.xml.parsers} came out when SAX 1 was still in use.
 *
 * <p>Both do the same with the handler: they plug it into the <b>four</b> points --content,
 * entities, errors and DTD-- because {@link DefaultHandler} implements the four interfaces. That is
 * where the class's convenience comes from: a single instance, and only the methods that matter are
 * overridden.
 */
public abstract class SAXParser {

    /** For the subclasses. */
    protected SAXParser() {
    }

    /**
     * Leaves the parser as newly created.
     *
     * @throws UnsupportedOperationException by default, as in {@link DocumentBuilder#reset}
     */
    public void reset() {
        throw new UnsupportedOperationException(
            "This SAXParser, \"" + this.getClass().getName()
                + "\", does not support the reset functionality.");
    }

    /**
     * Reads from a stream with a SAX 1 handler.
     *
     * @throws IllegalArgumentException if the stream is null
     * @deprecated see the class note; use the version with {@link DefaultHandler}
     */
    @Deprecated
    public void parse(InputStream is, HandlerBase hb) throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        parse(new InputSource(is), hb);
    }

    /**
     * Likewise, saying where it came from.
     *
     * @throws IllegalArgumentException if the stream is null
     * @deprecated see the class note
     */
    @Deprecated
    public void parse(InputStream is, HandlerBase hb, String systemId)
        throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        InputSource in = new InputSource(is);
        in.setSystemId(systemId);
        parse(in, hb);
    }

    /**
     * Reads from a stream.
     *
     * @throws IllegalArgumentException if the stream is null
     */
    public void parse(InputStream is, DefaultHandler dh) throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        parse(new InputSource(is), dh);
    }

    /**
     * Likewise, saying where it came from.
     *
     * @param systemId what relative references are resolved against
     * @throws IllegalArgumentException if the stream is null
     */
    public void parse(InputStream is, DefaultHandler dh, String systemId)
        throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        InputSource in = new InputSource(is);
        in.setSystemId(systemId);
        parse(in, dh);
    }

    /**
     * Reads from a URI with a SAX 1 handler.
     *
     * @throws IllegalArgumentException if the URI is null
     * @deprecated see the class note
     */
    @Deprecated
    public void parse(String uri, HandlerBase hb) throws SAXException, IOException {
        if (uri == null) {
            throw new IllegalArgumentException("uri cannot be null");
        }
        parse(new InputSource(uri), hb);
    }

    /**
     * Reads from a URI.
     *
     * @throws IllegalArgumentException if the URI is null
     */
    public void parse(String uri, DefaultHandler dh) throws SAXException, IOException {
        if (uri == null) {
            throw new IllegalArgumentException("uri cannot be null");
        }
        parse(new InputSource(uri), dh);
    }

    /**
     * Reads from a file with a SAX 1 handler.
     *
     * @throws IllegalArgumentException if the file is null
     * @deprecated see the class note
     */
    @Deprecated
    public void parse(File f, HandlerBase hb) throws SAXException, IOException {
        if (f == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        parse(new InputSource(f.toURI().toString()), hb);
    }

    /**
     * Reads from a file.
     *
     * <p>The system identifier comes from the absolute path; see {@link
     * DocumentBuilder#parse(File)}.
     *
     * @throws IllegalArgumentException if the file is null
     */
    public void parse(File f, DefaultHandler dh) throws SAXException, IOException {
        if (f == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        parse(new InputSource(f.toURI().toString()), dh);
    }

    /**
     * Reads from a source with a SAX 1 handler.
     *
     * @throws IllegalArgumentException if the source is null
     * @deprecated see the class note
     */
    @Deprecated
    public void parse(InputSource is, HandlerBase hb) throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputSource cannot be null");
        }
        Parser parser = this.getParser();
        if (hb != null) {
            parser.setDocumentHandler(hb);
            parser.setEntityResolver(hb);
            parser.setErrorHandler(hb);
            parser.setDTDHandler(hb);
        }
        parser.parse(is);
    }

    /**
     * Reads from a source.
     *
     * <p>It is where all the other SAX 2 {@code parse}s end up: the handler is plugged into the
     * four points and only then is it read.
     *
     * @throws IllegalArgumentException if the source is null
     */
    public void parse(InputSource is, DefaultHandler dh) throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputSource cannot be null");
        }
        XMLReader reader = this.getXMLReader();
        if (dh != null) {
            reader.setContentHandler(dh);
            reader.setEntityResolver(dh);
            reader.setErrorHandler(dh);
            reader.setDTDHandler(dh);
        }
        reader.parse(is);
    }

    /**
     * The SAX 1 parser behind it.
     *
     * @deprecated see the class note; use {@link #getXMLReader}
     */
    @Deprecated
    public abstract Parser getParser() throws SAXException;

    /** The SAX 2 one, which is the one that really does the work. */
    public abstract XMLReader getXMLReader() throws SAXException;

    /** Whether it tells namespaces apart. */
    public abstract boolean isNamespaceAware();

    /** Whether it validates against the document's DTD. */
    public abstract boolean isValidating();

    /**
     * Changes a property of the reader.
     *
     * @throws SAXNotRecognizedException if it does not know that name
     * @throws SAXNotSupportedException if it knows it but cannot change it now
     */
    public abstract void setProperty(String name, Object value)
        throws SAXNotRecognizedException, SAXNotSupportedException;

    /** The value of a property. */
    public abstract Object getProperty(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException;

    /**
     * The schema it validates against, or null.
     *
     * <p>The factory sets it; see {@link DocumentBuilder#getSchema}.
     *
     * @throws UnsupportedOperationException by default
     */
    public Schema getSchema() {
        throw new UnsupportedOperationException(
            "This parser does not support specification \"XML Schema\".");
    }

    /**
     * Whether it resolves XInclude.
     *
     * @throws UnsupportedOperationException by default; see {@link DocumentBuilder#isXIncludeAware}
     */
    public boolean isXIncludeAware() {
        throw new UnsupportedOperationException(
            "This parser does not support specification \"XInclude\".");
    }
}
