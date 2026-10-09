package javax.xml.transform.stax;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;

/**
 * KajiLibrary's javax.xml.transform.stax.StAXResult -- a StAX writer as the destination of a
 * transformation.
 *
 * <p>The mirror of {@link StAXSource}: it carries <b>one</b> of the two writers and the getter of
 * the other returns null.
 *
 * <p>It serves to chain without materializing anything in between: the output of a transformation
 * is written directly through the same writer the program was already writing with, instead of
 * going through a tree or through text.
 *
 * <p>{@link #setSystemId} throws, for the same reason as in {@link StAXSource}: the destination is
 * decided by the writer, not by whoever builds the {@code Result}.
 */
public class StAXResult implements Result {

    /** With this a {@code TransformerFactory} is asked whether it accepts this destination. */
    public static final String FEATURE = "http://javax.xml.transform.stax.StAXResult/feature";

    /** One of the two is null. */
    private XMLEventWriter eventWriter;

    private XMLStreamWriter streamWriter;

    /**
     * With an event writer.
     *
     * @throws IllegalArgumentException if it is null
     */
    public StAXResult(XMLEventWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException(
                "StAXResult(XMLEventWriter) with XMLEventWriter == null");
        }
        this.eventWriter = writer;
    }

    /**
     * With a stream writer.
     *
     * @throws IllegalArgumentException if it is null
     */
    public StAXResult(XMLStreamWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException(
                "StAXResult(XMLStreamWriter) with XMLStreamWriter == null");
        }
        this.streamWriter = writer;
    }

    /** The event writer, or null if it was built with the stream one. */
    public XMLEventWriter getXMLEventWriter() {
        return this.eventWriter;
    }

    /** The stream writer, or null if it was built with the event one. */
    public XMLStreamWriter getXMLStreamWriter() {
        return this.streamWriter;
    }

    /**
     * Cannot be changed.
     *
     * @throws UnsupportedOperationException always; see the class note
     */
    public void setSystemId(String systemId) {
        throw new UnsupportedOperationException(
            "StAXResult#setSystemId(systemId) cannot set the system identifier for a StAXResult");
    }

    /** Always null: the writer knows the destination. */
    public String getSystemId() {
        return null;
    }
}
