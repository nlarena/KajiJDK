package java.nio.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

import jdk.internal.io.Fs;

// KajiJDK's only provider: the `file` scheme's, over `jdk.internal.io.Fs`'s natives.
//
// **It is a facade over `Files`, not the other way round.** In the JDK the logic lives in the
// provider and `Files` delegates; here it was done the other way because there is no more than one
// provider and because `Files` is where somebody will go looking for the code. The consequence is
// that this class is almost all one line per method, which is exactly what one wants of a facade.
//
// **What it cannot do, and how it says so.** The methods the spec declares `abstract` have to be
// implemented whatever happens; the ones that need something this VM does not have throw
// `UnsupportedOperationException` with the reason written out. An `UnsupportedOperationException`
// is not a hole papered over: it says "this does not exist here" and cannot be mistaken for a
// result.
//
// **Two of them are now behind the facade, and that is worth naming.** `newDirectoryStream` and
// `getFileStore` still throw, but their natives arrived --`Fs.list` and the three `disk*`-- and
// `Files` uses them, so `Files.newDirectoryStream(dir)` works while
// `provider.newDirectoryStream(dir, filter)` does not. It is the facade that has not been wired,
// not the VM that cannot.
//
// A notable exception: `getFileAttributeView` returns `null`, which **is** the answer the spec
// requires when the view asked for is not available -- and here none is available.
final class KajiFileSystemProvider extends FileSystemProvider {

    static final KajiFileSystemProvider INSTANCE = new KajiFileSystemProvider();

    private KajiFileSystemProvider() {
    }

    public String getScheme() {
        return "file";
    }

    private static void checkUri(URI uri) {
        if (uri == null) {
            throw new NullPointerException();
        }
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase("file")) {
            throw new IllegalArgumentException("URI scheme is not \"file\"");
        }
    }

    /**
     * It always fails: the default filesystem is created with the VM and cannot be created again.
     * It is the same thing the JDK does.
     */
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        checkUri(uri);
        throw new FileSystemAlreadyExistsException();
    }

    public FileSystem getFileSystem(URI uri) {
        checkUri(uri);
        return KajiFileSystem.INSTANCE;
    }

    public Path getPath(URI uri) {
        checkUri(uri);
        return Path.of(uri);
    }

    public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        return Files.newInputStream(path, options);
    }

    public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
        return Files.newOutputStream(path, options);
    }

    /**
     * A channel over the file. `FileChannel.open` returns it, which is what this VM knows how to
     * open.
     *
     * <p>Returning the `FileChannel` directly --and not wrapping it to hide that it is one-- is
     * what the JDK does: the declared type is what is promised, the extra is what is given.
     */
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException {
        return FileChannel.open(path, options, attrs);
    }

    /** The same channel, with the type that promises more. See `newByteChannel`. */
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException {
        return FileChannel.open(path, options, attrs);
    }

    /**
     * It fails. This javadoc used to blame a missing native; `Fs.list` exists and
     * `Files.newDirectoryStream` uses it. What is missing is the wiring from here to there, so this
     * facade and `Files` disagree on the same question.
     */
    public DirectoryStream<Path> newDirectoryStream(Path dir,
            DirectoryStream.Filter<? super Path> filter) throws IOException {
        throw new UnsupportedOperationException("KajiJDK cannot list a directory");
    }

    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        Files.createDirectory(dir, attrs);
    }

    public void delete(Path path) throws IOException {
        Files.delete(path);
    }

    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        Files.copy(source, target, options);
    }

    public void move(Path source, Path target, CopyOption... options) throws IOException {
        Files.move(source, target, options);
    }

    /**
     * It answers `true` when the two paths are the same one written two ways, and fails otherwise.
     *
     * <p>Two equal paths are the same file, and the spec says that without looking at the disk. The
     * same holds after `normalize()` --`a/./b` and `a/b` are the same path-- **provided both are
     * absolute or both relative**: mixing them would force taking the relative one to absolute, and
     * with `user.dir` being `null` in this VM that settles nothing, it invents a prefix. Comparing
     * against an invented prefix can give a false `true`, which is worse than not answering.
     *
     * <p>For two paths that do **not** normalise the same, the file's identity has to be compared
     * --the inode, the file key. `Fs.canonical` gives it and `Files.isSameFile` uses it; this
     * facade has not been wired to it, so it fails here rather than guess.
     */
    public boolean isSameFile(Path path, Path path2) throws IOException {
        if (path.equals(path2)) {
            return true;
        }
        if (path.isAbsolute() == path2.isAbsolute()
                && path.normalize().equals(path2.normalize())) {
            return true;
        }
        throw new UnsupportedOperationException(
                "KajiJDK has no file key: two different paths cannot be compared");
    }

    /** The name starts with a dot. The definition and the why are in `Files.isHidden`. */
    public boolean isHidden(Path path) throws IOException {
        return Files.isHidden(path);
    }

    /** It fails. This javadoc used to blame missing volume statistics; the three `disk*` natives
     *  exist and `Files.getFileStore` builds a {@link KajiFileStore} out of them. What is missing
     *  is the wiring from here to there. */
    public FileStore getFileStore(Path path) throws IOException {
        throw new UnsupportedOperationException("KajiJDK models no file store");
    }

    /**
     * It checks existence and, if they are asked for, read and write.
     *
     * <p>`EXECUTE` fails: `stat` brings no execute bit, and accepting it in silence would say it
     * can be executed without having checked.
     */
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        String p = path.toString();
        int st = Fs.stat(p);
        if ((st & Fs.EXISTS) == 0) {
            throw new NoSuchFileException(p);
        }
        int i = 0;
        while (i < modes.length) {
            AccessMode m = modes[i];
            if (m == AccessMode.READ) {
                if ((st & Fs.CAN_READ) == 0) {
                    throw new AccessDeniedException(p);
                }
            } else if (m == AccessMode.WRITE) {
                if ((st & Fs.CAN_WRITE) == 0) {
                    throw new AccessDeniedException(p);
                }
            } else {
                throw new UnsupportedOperationException("no execute bit in stat");
            }
            i = i + 1;
        }
    }

    /**
     * Always `null`: no view is available. It is the answer the spec requires.
     *
     * <p>That there is no **view** and there is a `readAttributes` is not a contradiction: a view
     * reads and writes, and writing all three timestamps at once cannot be done. The detail is in
     * `Files.getFileAttributeView`.
     */
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type,
            LinkOption... options) {
        return Files.getFileAttributeView(path, type, options);
    }

    /** The `basic` view, taken from `stat`, `size` and `mtime`. See `Files.readAttributes`. */
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type,
            LinkOption... options) throws IOException {
        return Files.readAttributes(path, type, options);
    }

    /** The same, by attribute name. */
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options)
            throws IOException {
        return Files.readAttributes(path, attributes, options);
    }

    /** It fails: there is no native that writes an attribute by name. */
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options)
            throws IOException {
        throw new UnsupportedOperationException("KajiJDK cannot write file attributes");
    }

    /**
     * A `stat` and that is it.
     *
     * <p>The base class's --which builds a `checkAccess` and catches-- is overridden because here
     * there is a direct and cheaper answer.
     */
    public boolean exists(Path path, LinkOption... options) {
        return Files.exists(path, options);
    }
}
