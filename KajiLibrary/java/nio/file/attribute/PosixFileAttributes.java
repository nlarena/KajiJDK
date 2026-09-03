package java.nio.file.attribute;

import java.util.Set;

// Los atributos de POSIX: dueño, grupo y los nueve bits de permiso.
//
// Sin implementacion en KajiJDK por la misma razon que `BasicFileAttributes`: no hay nativo que
// devuelva uid/gid ni el modo del archivo. Notar que `stat` **si** sabe si se puede leer y escribir,
// pero eso es el permiso **efectivo para este proceso**, que no es lo mismo que los bits del archivo
// -- traducir uno al otro (por ejemplo, poner `OWNER_READ` porque el proceso puede leer) seria
// inventar informacion sobre el grupo y los otros.
public interface PosixFileAttributes extends BasicFileAttributes {

    /** El dueño. */
    UserPrincipal owner();

    /** El grupo. */
    GroupPrincipal group();

    /** Los permisos. */
    Set<PosixFilePermission> permissions();
}
