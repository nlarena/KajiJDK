package java.nio.file;

// A `FileSystem` that was already closed was used.
//
// KajiJDK's --`FileSystems.getDefault()`-- cannot be closed: `close()` does nothing and `isOpen()`
// is always `true`, just like the JDK's default one. So this exception never comes out of here; it
// exists for the code that catches it and for the providers that do close.
public class ClosedFileSystemException extends IllegalStateException {

    private static final long serialVersionUID = -8158336077256193488L;

    /** With no message. */
    public ClosedFileSystemException() {
    }
}
