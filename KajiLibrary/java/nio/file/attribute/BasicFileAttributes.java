package java.nio.file.attribute;

// The set of attributes every filesystem ought to be able to answer: three timestamps, four
// questions of type, the size and an identity key.
//
// **KajiJDK implements it.** This note used to say it did not, and that `Files.readAttributes`
// therefore did not exist: `Files.BasicAttrs` implements this interface and that method reads
// it. Five of the nine are real --`isRegularFile`, `isDirectory`, `isOther`, `size` and
// `lastModifiedTime`, out of `stat`, `size` and `mtime`. The other four are answered by the
// contract rather than invented: `lastAccessTime` and `creationTime` are the epoch, which is what
// the spec below requires when the filesystem does not keep the stamp; `fileKey()` is `null`, which
// the spec declares valid where there is no inode; and `isSymbolicLink()` is `false`, which
// `Files.isSymbolicLink` makes the answer for "cannot be determined".
public interface BasicFileAttributes {

    /** The last time the content was modified. */
    FileTime lastModifiedTime();

    /** The last time it was read. */
    FileTime lastAccessTime();

    /** When it was created. */
    FileTime creationTime();

    /** Whether it is a regular file. */
    boolean isRegularFile();

    /** Whether it is a directory. */
    boolean isDirectory();

    /** Whether it is a symbolic link. */
    boolean isSymbolicLink();

    /** Whether it is none of the three above. */
    boolean isOther();

    /** The size in bytes. */
    long size();

    /**
     * A key that identifies the file, or `null` if the system cannot give one.
     *
     * <p>`null` is a **valid** answer by spec, not a hole: it is what fits when there is nothing like
     * POSIX's inode.
     */
    Object fileKey();
}
