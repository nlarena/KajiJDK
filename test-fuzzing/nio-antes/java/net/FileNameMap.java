package java.net;

// From a file name to the MIME type that goes with it.
//
// The interface does not say where the mapping comes from -- it may be a table of extensions, the
// system's `mime.types`, or looking at the content. That is on purpose: whoever asks only wants the
// type.
public interface FileNameMap {

    /** The MIME type of {@code fileName}, or null if it could not be determined. */
    String getContentTypeFor(String fileName);
}
