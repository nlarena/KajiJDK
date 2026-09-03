package java.nio.file;

// El archivo esta pero el proceso no tiene permiso para lo que se pidio.
//
// Se elige sobre `NoSuchFileException` cuando `stat` dice que la ruta **existe** pero no trae la
// bandera de lectura o de escritura que hacia falta. Sin esa comprobacion previa las dos serian
// indistinguibles desde los nativos, y adivinar mandaria a buscar el problema al lugar equivocado.
public class AccessDeniedException extends FileSystemException {

    private static final long serialVersionUID = 4943049599949219617L;

    /** @param file el archivo al que no se pudo acceder, o `null` */
    public AccessDeniedException(String file) {
        super(file);
    }

    /** @param file el archivo; `other` el otro; `reason` el motivo. Cualquiera puede ser `null`. */
    public AccessDeniedException(String file, String other, String reason) {
        super(file, other, reason);
    }
}
