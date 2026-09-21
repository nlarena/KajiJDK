package javax.xml.parsers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.validation.Schema;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * KajiLibrary's javax.xml.parsers.DocumentBuilder -- reads XML and returns a tree.
 *
 * <p>It is the DOM face of parsing: the whole document is read and stays in memory as a {@link
 * Document} that can be walked in any direction. The other face is {@link SAXParser}, which
 * notifies by event and keeps nothing. Choosing between the two is not a matter of taste: DOM needs
 * several times the size of the file in memory, so for a large document that is walked only once,
 * SAX is the only option.
 *
 * <h2>One abstract parse and four shortcuts</h2>
 *
 * <p>The {@code parse}s that receive a stream, a file or a URI build an {@link InputSource} and
 * call the abstract one. It is worth looking at what they do with the <b>system identifier</b>: it
 * is what later allows a relative reference inside the document to be resolved, and that is why
 * {@code parse(File)} sets it from the absolute path and not from the one passed. An XML read from
 * a stream without an identifier cannot resolve anything relative, and that is the reason for the
 * overload that receives one separately. (The note said six shortcuts; there are four.)
 *
 * <h2>The defaults that throw</h2>
 *
 * <p>{@link #reset} and {@link #isXIncludeAware} have a body and throw {@link
 * UnsupportedOperationException}. It is deliberate and it is what the JDK does: they arrived after
 * version 1 of the class, and an old implementation that does not know them cannot answer them.
 * Returning false in {@code isXIncludeAware} would be worse -- it would assert something nobody
 * checked.
 */
public abstract class DocumentBuilder {

    /** For the subclasses. */
    protected DocumentBuilder() {
    }

    /**
     * Leaves the parser as newly created.
     *
     * @throws UnsupportedOperationException by default; see the class note
     */
    public void reset() {
        throw new UnsupportedOperationException(
            "This DocumentBuilder, \"" + this.getClass().getName()
                + "\", does not support the reset functionality.");
    }

    /**
     * Reads from a stream, without a system identifier.
     *
     * <p>A document read this way cannot resolve relative references; see the class note.
     *
     * @throws IllegalArgumentException if the stream is null
     */
    public Document parse(InputStream is) throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        return parse(new InputSource(is));
    }

    /**
     * Reads from a stream, saying where it came from.
     *
     * @param systemId what relative references are resolved against
     * @throws IllegalArgumentException if the stream is null
     */
    public Document parse(InputStream is, String systemId) throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        InputSource in = new InputSource(is);
        in.setSystemId(systemId);
        return parse(in);
    }

    /**
     * Reads from a URI.
     *
     * @throws IllegalArgumentException if the URI is null
     */
    public Document parse(String uri) throws SAXException, IOException {
        if (uri == null) {
            throw new IllegalArgumentException("URI cannot be null");
        }
        return parse(new InputSource(uri));
    }

    /**
     * Reads from a file.
     *
     * <p>The system identifier comes from the <b>absolute path</b>, not from the one passed:
     * otherwise, a document opened with a relative path could not resolve its own.
     *
     * @throws IllegalArgumentException if the file is null
     */
    public Document parse(File f) throws SAXException, IOException {
        if (f == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        return parse(new InputSource(f.toURI().toString()));
    }

    /** The only one to write: all the others end up here. */
    public abstract Document parse(InputSource is) throws SAXException, IOException;

    /** Whether it tells namespaces apart. */
    public abstract boolean isNamespaceAware();

    /** Whether it validates against the document's DTD. */
    public abstract boolean isValidating();

    /**
     * Who resolves the external entities.
     *
     * <p>Giving it one that rejects them is the defence against XXE, which is the classic attack on
     * this API: a document that declares an entity pointing to a local file and makes it appear in
     * the output.
     */
    public abstract void setEntityResolver(EntityResolver er);

    /** Who decides what to do with errors; without one, they go to standard error. */
    public abstract void setErrorHandler(ErrorHandler eh);

    /** An empty document, to build one by hand. */
    public abstract Document newDocument();

    /** The DOM implementation behind this parser. */
    public abstract DOMImplementation getDOMImplementation();

    /**
     * The schema it validates against, or null.
     *
     * <p>The factory sets it with {@code DocumentBuilderFactory.setSchema}, it is not set here: an
     * already built parser cannot change schema, because the schema decides how it is built.
     *
     * @throws UnsupportedOperationException by default; see the class note
     */
    public Schema getSchema() {
        throw new UnsupportedOperationException(
            "This parser does not support specification \"XML Schema\".");
    }

    /**
     * Whether it resolves XInclude.
     *
     * @throws UnsupportedOperationException by default; see the class note
     */
    public boolean isXIncludeAware() {
        throw new UnsupportedOperationException(
            "This parser does not support specification \"XInclude\".");
    }
}
