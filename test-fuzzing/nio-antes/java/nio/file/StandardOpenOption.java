package java.nio.file;

// The standard options for opening a file.
//
// **Which ones KajiJDK understands.** This VM's file model is "all at once": `Fs.readAllBytes` and
// `Fs.writeAllBytes(path, bytes, append)`. Over that, `Files.newOutputStream` and company honour
// `READ`, `WRITE`, `APPEND`, `CREATE`, `CREATE_NEW` and `TRUNCATE_EXISTING`, which are the ones
// that can be expressed with that single `append` parameter plus a prior `stat`.
//
// The other four --`DELETE_ON_CLOSE`, `SPARSE`, `SYNC`, `DSYNC`-- **cannot be honoured there and
// are rejected** with `UnsupportedOperationException` rather than ignored: a `SYNC` that does not
// really synchronise is exactly the kind of false promise that loses data, and accepting it in
// silence would be worse than not offering it. `newByteChannel` is the exception and accepts three
// of the four, because `FileChannel` writes to disk on every write -- see `Files`'s header.
public enum StandardOpenOption implements OpenOption {

    /** Open for reading. */
    READ,

    /** Open for writing. */
    WRITE,

    /** Always write at the end of what is already there. */
    APPEND,

    /** If it exists already and is opened for writing, leave it at zero bytes. */
    TRUNCATE_EXISTING,

    /** Create it if it does not exist. */
    CREATE,

    /** Create it, and fail if it was already there. */
    CREATE_NEW,

    /** Delete it on closing. KajiJDK's stream forms do not support it. */
    DELETE_ON_CLOSE,

    /** Ask the system to store it sparsely. KajiJDK does not support it. */
    SPARSE,

    /** Synchronise content and metadata with the disk on every write. KajiJDK's stream forms do not
     *  support it. */
    SYNC,

    /** Synchronise only the content. KajiJDK's stream forms do not support it. */
    DSYNC
}
