package java.nio.file.attribute;

// POSIX's nine permission bits, one per constant.
//
// **The order matters** and is not decorative: `PosixFilePermissions.toString()` walks `values()` to
// build `"rwxr-xr-x"`, so the constants go owner-group-others and within each triple
// read-write-execute, just as in the JDK. Changing the order would change the string.
public enum PosixFilePermission {

    /** The owner can read. */
    OWNER_READ,

    /** The owner can write. */
    OWNER_WRITE,

    /** The owner can execute (or traverse, if it is a directory). */
    OWNER_EXECUTE,

    /** The group can read. */
    GROUP_READ,

    /** The group can write. */
    GROUP_WRITE,

    /** The group can execute. */
    GROUP_EXECUTE,

    /** The others can read. */
    OTHERS_READ,

    /** The others can write. */
    OTHERS_WRITE,

    /** The others can execute. */
    OTHERS_EXECUTE
}
