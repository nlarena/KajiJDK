package java.nio.file;

// El sistema de archivos que se pidio no existe (todavia).
//
// La levanta `FileSystems.getFileSystem(URI)` para cualquier esquema que no sea `file`: KajiJDK
// tiene un solo proveedor y ninguno mas se puede instalar.
public class FileSystemNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 7999581764446402397L;

    /** Sin mensaje. */
    public FileSystemNotFoundException() {
    }

    /** @param msg el detalle */
    public FileSystemNotFoundException(String msg) {
        super(msg);
    }
}
