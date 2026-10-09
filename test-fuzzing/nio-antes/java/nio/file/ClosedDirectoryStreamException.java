package java.nio.file;

// A `DirectoryStream` that was already closed was used.
public class ClosedDirectoryStreamException extends IllegalStateException {

    private static final long serialVersionUID = 4228386650900895400L;

    /** With no message: the class's name says everything. */
    public ClosedDirectoryStreamException() {
    }
}
