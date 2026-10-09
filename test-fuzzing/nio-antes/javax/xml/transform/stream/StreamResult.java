package javax.xml.transform.stream;

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;

import javax.xml.transform.Result;

/**
 * KajiLibrary's javax.xml.transform.stream.StreamResult -- the result is written serialized.
 *
 * <p>The mirror of {@link StreamSource}, and the difference from the other {@link Result}
 * implementations is not one of form but of **who does the work**: writing to a tree or to an event
 * handler is handing over nodes, writing here is serializing --choosing the encoding, escaping the
 * `&amp;`s, deciding whether to indent--. That is why the properties of {@link
 * javax.xml.transform.OutputKeys} only take effect when the destination is this.
 *
 * <p>The three destinations are looked at in the same order as in the source: the {@link Writer},
 * then the {@link OutputStream}, and last the system identifier, which the processor opens itself.
 * And the same warning, inverted and worse: a `Writer` has already fixed the encoding, so {@code
 * OutputKeys.ENCODING} **cannot change it**, and on top of that the XML declaration that is emitted
 * is going to announce an encoding the `Writer` is not using. If the encoding matters, the
 * destination is the byte stream.
 */
public class StreamResult implements Result {

    /**
     * The name with which a factory is asked whether it accepts this kind of destination.
     *
     * <p>Read-only, like {@link StreamSource#FEATURE}: it is not an option that is turned on.
     */
    public static final String FEATURE = "http://javax.xml.transform.stream.StreamResult/feature";

    private String systemId;
    private OutputStream outputStream;
    private Writer writer;

    /** Empty, to be filled later with the `set`s. */
    public StreamResult() {
    }

    /**
     * Towards a byte stream. It is the destination that honours {@code OutputKeys.ENCODING}.
     *
     * @param outputStream where to write
     */
    public StreamResult(OutputStream outputStream) {
        setOutputStream(outputStream);
    }

    /**
     * Towards a character stream. Watch the encoding: see the header.
     *
     * @param writer where to write
     */
    public StreamResult(Writer writer) {
        setWriter(writer);
    }

    /**
     * Towards a URI, which the processor opens itself.
     *
     * @param systemId the URI of the destination
     */
    public StreamResult(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Towards a file. The URI is built with {@link #setSystemId(File)}.
     *
     * @param f the file
     */
    public StreamResult(File f) {
        setSystemId(f);
    }

    // ---- where the bytes go ---------------------------------------------------------------------

    /**
     * Sets the byte stream.
     *
     * @param outputStream where to write, or null
     */
    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /** The byte stream, or null. */
    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Sets the character stream, which wins over the byte one.
     *
     * @param writer where to write, or null
     */
    public void setWriter(Writer writer) {
        this.writer = writer;
    }

    /** The character stream, or null. */
    public Writer getWriter() {
        return writer;
    }

    // ---- identification ----------------------------------------------------------------------

    /**
     * Sets the URI of the destination.
     *
     * @param systemId the URI, or null
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Sets the URI of the destination from a file, converted to {@code file:}.
     *
     * <p>The same conversion as in {@link StreamSource#setSystemId(File)}, and for the same reason:
     * without percent-encoding, a path with spaces is not a URI. (The note pointed to a ceiling
     * shared with that method, in {@code java.net.URI} and {@code File}; it no longer holds, see
     * there.)
     *
     * @param f the file
     */
    public void setSystemId(File f) {
        this.systemId = f.toURI().toASCIIString();
    }

    /** The URI of the destination, or null. */
    public String getSystemId() {
        return systemId;
    }
}
