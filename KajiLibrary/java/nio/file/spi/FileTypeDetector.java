package java.nio.file.spi;

import java.io.IOException;
import java.nio.file.Path;

// The extension point for guessing a file's MIME type.
//
// **KajiJDK installs none.** This note used to add that `Files.probeContentType` therefore does not
// exist; it does, and it returns `null` for everything -- which is the correct answer with an empty
// chain, and its own javadoc says why. A serious detector looks at the extension against a table
// and/or the first bytes against known signatures; writing the table is easy, but the spec says the
// result comes from the **installed** detectors, and KajiJDK does not have the service mechanism
// that installs them. A `probeContentType` that returned `"text/plain"` for every `.txt` and `null`
// for the rest would be answering on behalf of a detector nobody registered. The abstract class is
// here so whoever wants to write one has something to inherit from.
public abstract class FileTypeDetector {

    /** For the subclasses. */
    protected FileTypeDetector() {
    }

    /**
     * `path`'s MIME type, or `null` if this detector does not recognise it.
     *
     * <p>`null` is a valid and expected answer: it means "I do not know", and lets the next detector
     * in the chain try.
     */
    public abstract String probeContentType(Path path) throws IOException;
}
