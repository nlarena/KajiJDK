package java.nio.file.attribute;

import java.io.IOException;
import java.util.Set;

// La vista `"posix"`: los atributos basicos, mas dueño, grupo y los nueve bits de permiso.
//
// Hereda de dos interfaces a la vez --`BasicFileAttributeView` por los tiempos,
// `FileOwnerAttributeView` por el dueño--, que es lo que hace que `getOwner()` este disponible sin
// redeclararlo.
//
// Sin implementacion en KajiJDK: ver la nota de `PosixFileAttributes` sobre por que los permisos
// efectivos que si sabe `stat` no alcanzan para reconstruir los bits del archivo.
public interface PosixFileAttributeView extends BasicFileAttributeView, FileOwnerAttributeView {

    /** Siempre `"posix"`. */
    String name();

    /** Los atributos, leidos de una sola vez. */
    PosixFileAttributes readAttributes() throws IOException;

    /** Cambia los nueve bits de permiso. */
    void setPermissions(Set<PosixFilePermission> perms) throws IOException;

    /** Cambia el grupo. */
    void setGroup(GroupPrincipal group) throws IOException;
}
