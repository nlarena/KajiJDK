package java.nio.file.attribute;

// How an ACL entry is inherited by what hangs off a directory. They only make sense on directories;
// on a regular file they are ignored.
public enum AclEntryFlag {

    /** The files created inside inherit it. */
    FILE_INHERIT,

    /** The subdirectories created inside inherit it. */
    DIRECTORY_INHERIT,

    /** The inheritance reaches one level and does not go on down. */
    NO_PROPAGATE_INHERIT,

    /** It is inherited but does not apply to the directory that carries it. */
    INHERIT_ONLY
}
