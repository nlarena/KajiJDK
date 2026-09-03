package java.nio.file;

// Se detecto un ciclo mientras se recorria el arbol de directorios.
//
// KajiJDK nunca la levanta: recorrer requiere listar directorios y no hay nativo que lo haga, asi
// que `Files.walkFileTree` no existe. El tipo esta para que la jerarquia este completa.
public class FileSystemLoopException extends FileSystemException {

    private static final long serialVersionUID = 4843039591949217617L;

    /** @param file la ruta donde se cerro el ciclo */
    public FileSystemLoopException(String file) {
        super(file);
    }
}
