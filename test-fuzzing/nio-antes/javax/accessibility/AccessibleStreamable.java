package javax.accessibility;

import java.awt.datatransfer.DataFlavor;
import java.io.InputStream;

/**
 * Implemented by what, besides being seen, can be **read as a stream of bytes**.
 *
 * <p>It serves so that an assistive technology can take the content in its native format --an
 * image, a document-- instead of having to rebuild it from the accessible description.
 */
public interface AccessibleStreamable {

    /** In which formats the content can be delivered. */
    DataFlavor[] getMimeTypes();

    /**
     * The content in that format.
     *
     * @return the stream, or `null` if the format is not supported
     */
    InputStream getStream(DataFlavor flavor);
}
