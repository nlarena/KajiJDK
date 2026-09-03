package java.nio.file;

// El archivo no existe.
//
// Es la que corresponde cuando `stat` dice que la ruta no esta. Notar que el nativo **no distingue**
// "no existe" de "no tengo permiso" --devuelve cero banderas en los dos casos-- asi que quien la
// levanta tiene que haber comprobado la existencia por separado antes de elegir entre esta y
// `AccessDeniedException`.
public class NoSuchFileException extends FileSystemException {

    private static final long serialVersionUID = -1390291775875351931L;

    /** @param file el archivo que no esta, o `null` */
    public NoSuchFileException(String file) {
        super(file);
    }

    /** @param file el archivo; `other` el otro; `reason` el motivo. Cualquiera puede ser `null`. */
    public NoSuchFileException(String file, String other, String reason) {
        super(file, other, reason);
    }
}
