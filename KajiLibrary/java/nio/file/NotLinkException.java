package java.nio.file;

// Se esperaba un enlace simbolico y la ruta apunta a otra cosa.
//
// KajiJDK nunca la levanta: no hay nativo que lea enlaces, asi que `Files.readSymbolicLink` --el
// unico que la tiraria-- no existe. El tipo esta porque es parte de la jerarquia y porque codigo
// que la atrapa tiene que poder compilar.
public class NotLinkException extends FileSystemException {

    private static final long serialVersionUID = -388655596416518021L;

    /** @param file la ruta que no era un enlace, o `null` */
    public NotLinkException(String file) {
        super(file);
    }

    /** @param file el archivo; `other` el otro; `reason` el motivo. Cualquiera puede ser `null`. */
    public NotLinkException(String file, String other, String reason) {
        super(file, other, reason);
    }
}
