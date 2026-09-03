package java.nio.file.attribute;

// Los nueve bits de permiso de POSIX, uno por constante.
//
// **El orden importa** y no es decorativo: `PosixFilePermissions.toString()` recorre `values()` para
// armar `"rwxr-xr-x"`, asi que las constantes van dueño-grupo-otros y dentro de cada terna
// lectura-escritura-ejecucion, igual que en el JDK. Cambiar el orden cambiaria la cadena.
public enum PosixFilePermission {

    /** El dueño puede leer. */
    OWNER_READ,

    /** El dueño puede escribir. */
    OWNER_WRITE,

    /** El dueño puede ejecutar (o atravesar, si es directorio). */
    OWNER_EXECUTE,

    /** El grupo puede leer. */
    GROUP_READ,

    /** El grupo puede escribir. */
    GROUP_WRITE,

    /** El grupo puede ejecutar. */
    GROUP_EXECUTE,

    /** Los demas pueden leer. */
    OTHERS_READ,

    /** Los demas pueden escribir. */
    OTHERS_WRITE,

    /** Los demas pueden ejecutar. */
    OTHERS_EXECUTE
}
