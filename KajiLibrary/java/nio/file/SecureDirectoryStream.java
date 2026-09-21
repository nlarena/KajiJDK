package java.nio.file;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Set;

// A `DirectoryStream` that can be operated on **relative to the open directory**, without resolving
// the full path again.
//
// **What it protects against.** If between listing `/tmp/x` and deleting `/tmp/x/y` somebody
// replaces `/tmp/x` with a link elsewhere, deleting by absolute path deletes the wrong file. This
// type operates against the already-open directory, so the change underneath does not redirect it.
//
// **The interface is complete.** `newByteChannel` was omitted while
// `java.nio.channels.SeekableByteChannel` did not exist in this library --a method returning a type
// that is not written cannot be declared--; it exists now, with `FileChannel` behind it, so the
// method is declared.
//
// KajiJDK produces none. This note used to blame there being no working `DirectoryStream`; there is
// one (`KajiDirectoryStream`, over `Fs.list`), and what is missing is the *secure* part --
// operating against an already-open directory needs a directory handle, and `Fs` works by path. It
// is an interface with no implementations, and that is right: it is the type the signatures name.
//
// @param <T> the entries' type
public interface SecureDirectoryStream<T> extends DirectoryStream<T> {

    /** It opens a subdirectory relative to this one. */
    SecureDirectoryStream<T> newDirectoryStream(T path, LinkOption... options) throws IOException;

    /**
     * It opens a channel over an entry relative to this directory.
     *
     * <p>With neither `CREATE` nor `CREATE_NEW` in `options` the file has to exist; with
     * `CREATE_NEW` the creation is atomic with respect to this open directory, which is what this
     * type protects.
     */
    SeekableByteChannel newByteChannel(T path, Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException;

    /** It deletes a file relative to this directory. */
    void deleteFile(T path) throws IOException;

    /** It deletes an (empty) subdirectory relative to this one. */
    void deleteDirectory(T path) throws IOException;

    /** It moves an entry from this directory to another, also open. */
    void move(T srcpath, SecureDirectoryStream<T> targetdir, T targetpath) throws IOException;

    /** A view of the open directory's own attributes. */
    <V extends FileAttributeView> V getFileAttributeView(Class<V> type);

    /** A view of the attributes of an entry relative to this directory. */
    <V extends FileAttributeView> V getFileAttributeView(T path, Class<V> type,
            LinkOption... options);
}
