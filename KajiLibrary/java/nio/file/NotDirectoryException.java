package java.nio.file;

// A directory was expected and the path points at something else.
public class NotDirectoryException extends FileSystemException {

    private static final long serialVersionUID = -9011457427178200199L;

    /** @param file the path that was not a directory, or `null` */
    public NotDirectoryException(String file) {
        super(file);
    }
}
