package javax.xml.transform.stream;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.transform.Source;

/**
 * KajiLibrary's javax.xml.transform.stream.StreamSource -- an XML document arriving as bytes.
 *
 * <p>It is the implementation of {@link Source} that assumes nothing about the document: there is
 * no tree, there are no events, there is an unparsed stream. That is why it is the cheapest --the
 * processor parses only once, its own way-- and the only one that serves when the document has not
 * been read yet.
 *
 * <p>There are **three** ways of saying where the bytes come from, and the order in which the
 * processor looks at them is part of the contract and not a detail: first the {@link Reader}, then
 * the {@link InputStream}, and last the system identifier. Setting two is legal and the one higher
 * up wins; setting only the URI makes the processor open it itself.
 *
 * <p>That the `Reader` beats the `InputStream` has a consequence that bites: a `Reader` has already
 * decided the encoding, so the document's `&lt;?xml encoding="..."?&gt;` declaration **cannot be
 * honoured**. With an `InputStream` the parser reads it and chooses; with a `Reader` it arrives too
 * late. When the encoding matters, what is passed is the byte stream.
 *
 * <p>The system identifier is still needed even if the bytes are already there: it is the base URI
 * against which the document's relative `href`s are resolved. A stream without a URI is a document
 * that cannot have relative references.
 */
public class StreamSource implements Source {

    /**
     * The name with which a factory is asked whether it accepts this kind of source.
     *
     * <p>It is never passed to {@code setFeature}: it is read-only, and the `true` it returns means
     * "it can read a StreamSource", not an option that is turned on.
     */
    public static final String FEATURE = "http://javax.xml.transform.stream.StreamSource/feature";

    private String publicId;
    private String systemId;
    private InputStream inputStream;
    private Reader reader;

    /**
     * Empty, to be filled later with the `set`s.
     *
     * <p>It exists because there is code that builds the source in several steps --it receives the
     * URI from one place and the stream from another-- and cannot pass everything through the
     * constructor.
     */
    public StreamSource() {
    }

    /**
     * From a byte stream.
     *
     * @param inputStream where to read from
     */
    public StreamSource(InputStream inputStream) {
        setInputStream(inputStream);
    }

    /**
     * From a byte stream, with the base URI.
     *
     * @param inputStream where to read from
     * @param systemId the base URI for relative references
     */
    public StreamSource(InputStream inputStream, String systemId) {
        setInputStream(inputStream);
        setSystemId(systemId);
    }

    /**
     * From a character stream. Watch the encoding: see the header.
     *
     * @param reader where to read from
     */
    public StreamSource(Reader reader) {
        setReader(reader);
    }

    /**
     * From a character stream, with the base URI.
     *
     * @param reader where to read from
     * @param systemId the base URI for relative references
     */
    public StreamSource(Reader reader, String systemId) {
        setReader(reader);
        setSystemId(systemId);
    }

    /**
     * From a URI, which the processor opens itself.
     *
     * @param systemId the URI of the document
     */
    public StreamSource(String systemId) {
        this.systemId = systemId;
    }

    /**
     * From a file. The URI is built with {@link #setSystemId(File)}.
     *
     * @param f the file
     */
    public StreamSource(File f) {
        setSystemId(f);
    }

    // ---- where the bytes come from --------------------------------------------------------------

    /**
     * Sets the byte stream.
     *
     * @param inputStream where to read from, or null
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /** The byte stream, or null. */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Sets the character stream, which wins over the byte one.
     *
     * @param reader where to read from, or null
     */
    public void setReader(Reader reader) {
        this.reader = reader;
    }

    /** The character stream, or null. */
    public Reader getReader() {
        return reader;
    }

    // ---- identification ----------------------------------------------------------------------

    /**
     * Sets the public identifier.
     *
     * <p>It is purely informative --it serves for error messages-- because the processor resolves
     * by the URI. That it exists anyway is an inheritance from SAX, where a catalog can map it.
     *
     * @param publicId the identifier, or null
     */
    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    /** The public identifier, or null. */
    public String getPublicId() {
        return publicId;
    }

    /**
     * Sets the base URI.
     *
     * @param systemId the URI, or null
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /** The base URI, or null. */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Sets the base URI from a file, converted to {@code file:}.
     *
     * <p>The conversion goes through {@link File#toURI()} and then to ASCII, which is what is
     * needed: a URI with a space or an n with a tilde is not a valid URI until those characters are
     * percent-encoded, and a relative {@code href} resolved against an invalid base gives nothing.
     *
     * <p>The JDK does not delegate to {@code File.toURI()} here: it does its own path-to-URI
     * conversion. Delegating is right anyway --a single source of truth--. The note said this
     * carried two defects of the classes it depends on: that {@code java.net.URI} does not
     * percent-encode and that {@code File.getAbsolutePath()} cannot resolve a relative path because
     * {@code user.dir} is not defined in this VM. Neither holds any more: run on KajiJVM, {@code
     * File("C:/tmp/a b/c.xml")} gives {@code file:/C:/tmp/a%20b/c.xml}, the same as the JDK, and a
     * relative path resolves against {@code user.dir}.
     *
     * @param f the file
     */
    public void setSystemId(File f) {
        this.systemId = f.toURI().toASCIIString();
    }

    // ---- empty or not ---------------------------------------------------------------------------

    /**
     * Whether this source has no document at all.
     *
     * <p>Empty is **both things at once**: no bytes to read, and no URI to open. And the URI counts
     * by {@code null}, not by its content: the empty string is a URI --a bad one, but set on
     * purpose-- and a source carrying it is not a source with nothing.
     */
    public boolean isEmpty() {
        return streamEmpty() && systemId == null;
    }

    /**
     * Whether the stream, if there is one, has not a single character.
     *
     * <p>Looking at this **without consuming anything** is the whole problem, because whoever asks
     * "is it empty?" expects to be able to read it whole afterwards. It is solved with
     * `mark`/`reset`: mark, read one character, go back.
     *
     * <p>A stream that does not support marks cannot be looked at without breaking it, and there
     * the answer is **"it is not empty"**. It is not an optimistic assumption but the only safe one
     * of the two: saying it is empty would make the caller discard it, and a lost document is worse
     * than an empty document that is still attempted.
     */
    private boolean streamEmpty() {
        boolean empty = true;
        if (inputStream != null) {
            // The mark is checked **before** reading, not after: on a stream that does not support
            // it, `mark` does nothing and the test `read` would eat the document's first byte --
            // just what this method promises not to do.
            //
            // And `catch (Throwable)` instead of `catch (IOException)`. The note said this
            // library's `InputStream` does not declare `IOException` in `read`/`mark`/`reset`, so
            // naming it would not compile here; `read()` and `reset()` do declare it now.
            // `Throwable` still covers whatever the stream throws.
            if (!inputStream.markSupported()) {
                return false;
            }
            try {
                inputStream.mark(1);
                empty = (inputStream.read() == -1);
                inputStream.reset();
            } catch (Throwable cannotPeek) {
                empty = false;
            }
        }
        if (reader != null) {
            if (!reader.markSupported()) {
                return false;
            }
            try {
                reader.mark(1);
                empty = (reader.read() == -1);
                reader.reset();
            } catch (Throwable cannotPeek) {
                empty = false;
            }
        }
        return empty;
    }
}
