package java.nio.file;

// The filesystem asked for does not exist (yet).
//
// `FileSystems.getFileSystem(URI)` throws it for any scheme that is not `file`: KajiJDK has a
// single provider and no other can be installed.
public class FileSystemNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 7999581764446402397L;

    /** With no message. */
    public FileSystemNotFoundException() {
    }

    /** @param msg the detail */
    public FileSystemNotFoundException(String msg) {
        super(msg);
    }
}
