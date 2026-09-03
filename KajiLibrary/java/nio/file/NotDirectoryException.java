package java.nio.file;

// Se esperaba un directorio y la ruta apunta a otra cosa.
public class NotDirectoryException extends FileSystemException {

    private static final long serialVersionUID = -9011457427178200199L;

    /** @param file la ruta que no era un directorio, o `null` */
    public NotDirectoryException(String file) {
        super(file);
    }
}
