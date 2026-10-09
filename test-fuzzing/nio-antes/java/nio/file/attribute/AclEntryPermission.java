package java.nio.file.attribute;

// The permissions of an ACL entry, in the NFSv4 model Windows uses.
//
// The last three --`LIST_DIRECTORY`, `ADD_FILE`, `ADD_SUBDIRECTORY`-- are **not enum constants of
// their own**: they are aliases of `READ_DATA`, `WRITE_DATA` and `APPEND_DATA` under the name that
// fits when the object is a directory. They go as `static final` fields pointing at the same
// instance, just as in the JDK; that is why `values()` returns fourteen and not seventeen.
//
// **Mind that today the three are `null` when this is compiled with our own `javac`.** It is not a
// problem in the source --the JDK's `javac` compiles it correctly-- but in the emission: the
// `<clinit>` comes out with these three fields' `putstatic` **before** the `new`s that build the
// constants, the other way round from what the JLS requires (§12.4.2: the enum constants first, then
// the static initialisers in textual order). The source is left correct and the bug reported; when
// the emitter orders it right, this works without touching anything.
public enum AclEntryPermission {

    /** Read the file's content. */
    READ_DATA,

    /** Write the content, possibly overwriting what is there. */
    WRITE_DATA,

    /** Append at the end. */
    APPEND_DATA,

    /** Read the named attributes. */
    READ_NAMED_ATTRS,

    /** Write the named attributes. */
    WRITE_NAMED_ATTRS,

    /** Execute the file. */
    EXECUTE,

    /** Delete a child of a directory. */
    DELETE_CHILD,

    /** Read the basic attributes. */
    READ_ATTRIBUTES,

    /** Write the basic attributes. */
    WRITE_ATTRIBUTES,

    /** Delete the object. */
    DELETE,

    /** Read the ACL. */
    READ_ACL,

    /** Write the ACL. */
    WRITE_ACL,

    /** Change the owner. */
    WRITE_OWNER,

    /** Use the object as a local synchroniser. */
    SYNCHRONIZE;

    /** List a directory: the same permission as `READ_DATA`, under the directory name. */
    public static final AclEntryPermission LIST_DIRECTORY = READ_DATA;

    /** Create a file in a directory: the same permission as `WRITE_DATA`. */
    public static final AclEntryPermission ADD_FILE = WRITE_DATA;

    /** Create a subdirectory: the same permission as `APPEND_DATA`. */
    public static final AclEntryPermission ADD_SUBDIRECTORY = APPEND_DATA;
}
