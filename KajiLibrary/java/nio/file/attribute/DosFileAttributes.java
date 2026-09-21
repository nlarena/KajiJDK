package java.nio.file.attribute;

// The four bits inherited from DOS that Windows still keeps per file.
//
// KajiJDK cannot read them: `jdk.internal.io.Fs`'s `stat` returns
// exists/file/directory/readable/writable and nothing else. The interface exists --it is the type
// `DosFileAttributeView.readAttributes()` returns-- but there is no implementation.
public interface DosFileAttributes extends BasicFileAttributes {

    /** Whether it is marked read-only. */
    boolean isReadOnly();

    /** Whether it is hidden. */
    boolean isHidden();

    /** Whether the archive bit is set. */
    boolean isArchive();

    /** Whether it is a system file. */
    boolean isSystem();
}
