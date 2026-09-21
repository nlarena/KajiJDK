package java.nio.file;

// Creating a filesystem that was already created was asked for.
//
// `FileSystems.newFileSystem(URI, ...)` throws it for the `file` scheme: the default filesystem is
// created with the VM and cannot be created again.
public class FileSystemAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = -5438419127181131148L;

    /** With no message. */
    public FileSystemAlreadyExistsException() {
    }

    /** @param msg the detail */
    public FileSystemAlreadyExistsException(String msg) {
        super(msg);
    }
}
