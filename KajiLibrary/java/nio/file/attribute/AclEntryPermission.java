package java.nio.file.attribute;

// Los permisos de una entrada de ACL, en el modelo NFSv4 que usa Windows.
//
// Las tres ultimas --`LIST_DIRECTORY`, `ADD_FILE`, `ADD_SUBDIRECTORY`-- **no son constantes de enum
// propias**: son alias de `READ_DATA`, `WRITE_DATA` y `APPEND_DATA` con el nombre que corresponde
// cuando el objeto es un directorio. Van como campos `static final` que apuntan a la misma
// instancia, igual que en el JDK; por eso `values()` devuelve catorce y no diecisiete.
//
// **Ojo: hoy los tres valen `null` cuando esto se compila con el `javac` propio.** No es un problema
// del fuente --el `javac` del JDK lo compila bien-- sino de la emision: el `<clinit>` sale con los
// `putstatic` de estos tres campos **antes** de los `new` que construyen las constantes, al reves de
// lo que manda la JLS (§12.4.2: primero las constantes del enum, despues los inicializadores
// estaticos en orden textual). Se deja el fuente correcto y el bug reportado; cuando el emisor
// ordene bien, esto anda sin tocar nada.
public enum AclEntryPermission {

    /** Leer el contenido del archivo. */
    READ_DATA,

    /** Escribir el contenido, pudiendo pisar lo que hay. */
    WRITE_DATA,

    /** Agregar al final. */
    APPEND_DATA,

    /** Leer los atributos con nombre. */
    READ_NAMED_ATTRS,

    /** Escribir los atributos con nombre. */
    WRITE_NAMED_ATTRS,

    /** Ejecutar el archivo. */
    EXECUTE,

    /** Borrar un hijo de un directorio. */
    DELETE_CHILD,

    /** Leer los atributos basicos. */
    READ_ATTRIBUTES,

    /** Escribir los atributos basicos. */
    WRITE_ATTRIBUTES,

    /** Borrar el objeto. */
    DELETE,

    /** Leer la ACL. */
    READ_ACL,

    /** Escribir la ACL. */
    WRITE_ACL,

    /** Cambiar el dueño. */
    WRITE_OWNER,

    /** Usar el objeto como sincronizador local. */
    SYNCHRONIZE;

    /** Listar un directorio: el mismo permiso que `READ_DATA`, con el nombre de directorio. */
    public static final AclEntryPermission LIST_DIRECTORY = READ_DATA;

    /** Crear un archivo en un directorio: el mismo permiso que `WRITE_DATA`. */
    public static final AclEntryPermission ADD_FILE = WRITE_DATA;

    /** Crear un subdirectorio: el mismo permiso que `APPEND_DATA`. */
    public static final AclEntryPermission ADD_SUBDIRECTORY = APPEND_DATA;
}
