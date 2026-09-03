package java.nio.file;

// Se quiso borrar un directorio que todavia tiene cosas adentro.
//
// KajiJDK la puede levantar de verdad: el nativo `Fs.delete` borra directorios **solo si estan
// vacios** --a proposito, para que un `delete()` sobre el directorio equivocado no sea una perdida
// de datos-- y devuelve `false` cuando no puede. `Files.delete` traduce ese `false` sobre un
// directorio existente a esta excepcion.
public class DirectoryNotEmptyException extends FileSystemException {

    private static final long serialVersionUID = 3056667871802779003L;

    /** @param dir el directorio que no estaba vacio, o `null` */
    public DirectoryNotEmptyException(String dir) {
        super(dir);
    }
}
