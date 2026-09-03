package java.nio.file;

// Se pidio crear un sistema de archivos que ya estaba creado.
//
// La levanta `FileSystems.newFileSystem(URI, ...)` para el esquema `file`: el sistema por omision se
// crea con la VM y no se puede volver a crear.
public class FileSystemAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = -5438419127181131148L;

    /** Sin mensaje. */
    public FileSystemAlreadyExistsException() {
    }

    /** @param msg el detalle */
    public FileSystemAlreadyExistsException(String msg) {
        super(msg);
    }
}
