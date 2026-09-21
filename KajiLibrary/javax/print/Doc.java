package javax.print;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import javax.print.attribute.DocAttributeSet;

/**
 * KajiLibrary's javax.print.Doc -- a document to print.
 *
 * <p>It brings together three things: the data, the {@link DocFlavor} saying what type it is, and
 * the attributes that hold only for this document.
 *
 * <h2>The three accessors to the data</h2>
 *
 * <p>{@link #getPrintData} returns the data in its declared form. The other two are shortcuts for
 * the service, and <b>return null if they do not apply</b>:
 *
 * <ul>
 *   <li>{@link #getReaderForText} only if the data is character text;
 *   <li>{@link #getStreamForBytes} only if the data is bytes.
 * </ul>
 *
 * <p>Returning null is right, not an error. A service tries the one that suits it and if it is
 * given null uses {@code getPrintData}.
 *
 * <h2>It is read only once</h2>
 *
 * <p>The three methods have to return <b>the same</b> object on each call, not a new one. It is
 * what allows the data to be a stream that cannot be rewound. The flip side is that a {@code Doc}
 * can be printed only once.
 */
public interface Doc {

    /** What type the data is. */
    DocFlavor getDocFlavor();

    /**
     * The data, in the class the format declares.
     *
     * @throws IOException if the data is a stream and it could not be opened
     */
    Object getPrintData() throws IOException;

    /** This document's own attributes, or null. */
    DocAttributeSet getAttributes();

    /**
     * The data as characters, or null if it is not text. Always the same reader.
     *
     * @throws IOException if it could not be opened
     */
    Reader getReaderForText() throws IOException;

    /**
     * The data as bytes, or null if it is not. Always the same stream.
     *
     * @throws IOException if it could not be opened
     */
    InputStream getStreamForBytes() throws IOException;
}
