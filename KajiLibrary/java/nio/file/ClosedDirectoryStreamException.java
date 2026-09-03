package java.nio.file;

// Se uso un `DirectoryStream` que ya estaba cerrado.
public class ClosedDirectoryStreamException extends IllegalStateException {

    private static final long serialVersionUID = 4228386650900895400L;

    /** Sin mensaje: el nombre de la clase ya lo dice todo. */
    public ClosedDirectoryStreamException() {
    }
}
