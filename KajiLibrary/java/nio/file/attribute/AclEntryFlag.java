package java.nio.file.attribute;

// Como se hereda una entrada de ACL hacia lo que cuelga de un directorio. Solo tienen sentido sobre
// directorios; sobre un archivo comun se ignoran.
public enum AclEntryFlag {

    /** La heredan los archivos que se creen adentro. */
    FILE_INHERIT,

    /** La heredan los subdirectorios que se creen adentro. */
    DIRECTORY_INHERIT,

    /** La herencia llega un nivel y no sigue bajando. */
    NO_PROPAGATE_INHERIT,

    /** Se hereda pero no se aplica al directorio que la lleva. */
    INHERIT_ONLY
}
