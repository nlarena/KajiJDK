package java.nio.file;

// `Files.copy`'s and `Files.move`'s standard options.
//
// **KajiJDK honours `REPLACE_EXISTING` and rejects the other two.** `COPY_ATTRIBUTES` asks for the
// timestamps and the permissions to be carried over, and there is no native that reads or writes
// the permissions; `ATOMIC_MOVE` asks for a guarantee a move made of copy-and-delete cannot give.
// In both cases the exception the spec already foresees is thrown --`UnsupportedOperationException`
// and `AtomicMoveNotSupportedException`-- rather than accepting them and not honouring them.
public enum StandardCopyOption implements CopyOption {

    /** If the target exists, overwrite it. */
    REPLACE_EXISTING,

    /** Copy the attributes too. KajiJDK does not support it. */
    COPY_ATTRIBUTES,

    /** Move as an atomic operation. KajiJDK does not support it. */
    ATOMIC_MOVE
}
