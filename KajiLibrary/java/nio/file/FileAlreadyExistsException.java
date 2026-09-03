package java.nio.file;

// El archivo ya existe y la operacion pedia crearlo.
//
// La levantan `Files.createFile`, `Files.createDirectory` y las copias sin `REPLACE_EXISTING`. En
// KajiJDK la comprobacion es un `stat` previo, no una creacion atomica -- ver la nota de
// `Files.createFile`.
public class FileAlreadyExistsException extends FileSystemException {

    private static final long serialVersionUID = 7579540934498831181L;

    /** @param file el archivo que ya estaba, o `null` */
    public FileAlreadyExistsException(String file) {
        super(file);
    }

    /** @param file el archivo; `other` el otro; `reason` el motivo. Cualquiera puede ser `null`. */
    public FileAlreadyExistsException(String file, String other, String reason) {
        super(file, other, reason);
    }
}
