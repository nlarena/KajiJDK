package java.nio.file.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

// The seam a filesystem plugs into: everything `Files` does, it does by calling a provider.
//
// **How the class is split, which is what explains which methods are `abstract`.** What is
// `abstract` is what no provider can inherit from another --deleting, copying, creating a directory,
// reading attributes--; what is concrete is the combinations built out of that: `deleteIfExists` is
// `delete()` catching `NoSuchFileException`, and `readAttributesIfExists` is the same with
// `readAttributes`. Writing them once here instead of in each provider is the point of the class.
//
// **The three channel methods are here, and `newByteChannel` is why they matter.** It is `abstract`
// --as in the JDK-- and it is where `newInputStream` and `newOutputStream` come from by default: a
// provider that knows how to open a channel already knows how to open both streams, and does not
// have to write them. `newFileChannel` and `newAsynchronousFileChannel` are concrete and fail by
// default, also as in the JDK: they are the optional promise that the returned channel can on top of
// that be memory-mapped or locked, and not every provider can honour that.
//
// The class's whole API is here. What a concrete provider can do with it is its own business: this
// VM's opens channels over `Fs`'s natives --which read and write the whole file at once-- and that
// is why it does not offer the asynchronous one, where the signature would promise a parallelism
// that is not there. See `java.nio.channels.AsynchronousFileChannel`'s header.
public abstract class FileSystemProvider {

    /** For the subclasses. */
    protected FileSystemProvider() {
    }

    /**
     * The installed providers, with the default one first.
     *
     * <p>KajiJDK returns **exactly one**: the `file` scheme's. There is no service loading --nothing
     * that discovers providers on the class path-- so the list cannot grow, and that is why it is
     * immutable rather than a defensive copy.
     */
    public static List<FileSystemProvider> installedProviders() {
        return Collections.singletonList(FileSystems.getDefault().provider());
    }

    /** The URI scheme this provider serves: `"file"`, `"jar"`, ... */
    public abstract String getScheme();

    /** It creates a fresh filesystem for `uri`. */
    public abstract FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException;

    /** The filesystem that already exists for `uri`. */
    public abstract FileSystem getFileSystem(URI uri);

    /** The path `uri` names. */
    public abstract Path getPath(URI uri);

    /**
     * It creates a filesystem out of a file --typically a ZIP.
     *
     * <p>By default it fails: it is the overload that only makes sense for container providers.
     */
    public FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * It opens `path` for reading.
     *
     * <p>Concrete because it is built on a read channel wrapped in a stream. No provider needs to
     * write it, and that is why it is here once.
     *
     * <p>`APPEND` and `WRITE` are rejected rather than ignored: asking that something be written
     * that is going to be returned as a read stream is not a redundant option, it is a confusion
     * about what is being opened, and in silence it is found out late.
     *
     * <p><strong>The channel comes from `Files.newByteChannel`, not from
     * `this.newByteChannel`</strong>, and it is not an oversight: it is what the JDK does --checked
     * by disassembling `FileSystemProvider`-- and it is copied so the two VMs answer the same. Note
     * that `newOutputStream`, four lines below, does call `this`: the asymmetry is the JDK's, not
     * this file's. It comes to the same for any real provider, because the path belongs to the
     * provider that opens it; it only shows if a provider is handed a foreign path, and there the JDK
     * attends to the path. **Do not "fix" this without measuring against the JDK again**: changing it
     * to `this` is an observable divergence.
     *
     * @throws UnsupportedOperationException if `APPEND` or `WRITE` is asked for
     */
    public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        for (OpenOption opt : options) {
            if (opt == StandardOpenOption.APPEND || opt == StandardOpenOption.WRITE) {
                throw new UnsupportedOperationException("'" + opt + "' not allowed");
            }
        }
        return Channels.newInputStream(Files.newByteChannel(path, options));
    }

    /**
     * It opens `path` for writing, also over `newByteChannel`.
     *
     * <p>With no options what holds in `Files` holds: create if it is not there and truncate if it
     * was. `READ` is `IllegalArgumentException` --and not `UnsupportedOperationException` like
     * `newInputStream`'s symmetric case-- because that is how the JDK tells them apart: there the
     * option is impossible to honour, here the argument contradicts the operation.
     *
     * @throws IllegalArgumentException if `READ` is asked for
     */
    public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
        Set<OpenOption> opts = new HashSet<OpenOption>();
        if (options.length == 0) {
            opts.add(StandardOpenOption.CREATE);
            opts.add(StandardOpenOption.TRUNCATE_EXISTING);
        } else {
            for (OpenOption opt : options) {
                if (opt == StandardOpenOption.READ) {
                    throw new IllegalArgumentException("READ not allowed");
                }
                opts.add(opt);
            }
        }
        opts.add(StandardOpenOption.WRITE);
        return Channels.newOutputStream(this.newByteChannel(path, opts));
    }

    /**
     * It opens a channel over `path`: the operation the other ways of reading and writing come out
     * of.
     *
     * <p>It is `abstract` and it is the only one of the three channel methods that is, because it is
     * the minimum: a provider that can answer it already gives its users both streams and everything
     * `Files` builds on top. The other two promise more and are therefore optional.
     */
    public abstract SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException;

    /**
     * Like `newByteChannel`, but promising a `FileChannel`.
     *
     * <p>By default it fails, and the difference from `newByteChannel` is what explains their being
     * two methods: a `FileChannel` is not just a channel with a position, it is one that on top of
     * that can be memory-mapped and locked against other processes. A ZIP provider can give the first
     * and not the second, so the promise is asked for separately.
     */
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * An asynchronous channel over `path`, running the operations on `executor`.
     *
     * <p>By default it fails. This VM's provider does **not** override it: the file natives are
     * synchronous and read the whole file at once, so the only thing that could be returned is a
     * facade that runs synchronous operations on another thread --with no cancellation and no real
     * parallelism. Returning that would be promising exactly the properties one chooses this API
     * for. The full reasoning is in `AsynchronousFileChannel`'s header.
     */
    public AsynchronousFileChannel newAsynchronousFileChannel(Path path,
            Set<? extends OpenOption> options, ExecutorService executor,
            FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }

    /** It opens a directory to walk it, keeping the entries `filter` accepts. */
    public abstract DirectoryStream<Path> newDirectoryStream(Path dir,
            DirectoryStream.Filter<? super Path> filter) throws IOException;

    /** It creates a directory. */
    public abstract void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException;

    /** It creates a symbolic link. By default it fails: not every system has them. */
    public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    /** It creates a hard link. By default it fails. */
    public void createLink(Path link, Path existing) throws IOException {
        throw new UnsupportedOperationException();
    }

    /** It deletes a file, or an empty directory. */
    public abstract void delete(Path path) throws IOException;

    /**
     * It deletes if it is there; it returns whether it deleted anything.
     *
     * <p>Concrete because it is `delete()` catching the "was not there" one -- no provider needs to
     * write it. Note that **it is not atomic**: between the attempt and the failure somebody could
     * have created the file.
     */
    public boolean deleteIfExists(Path path) throws IOException {
        try {
            this.delete(path);
            return true;
        } catch (NoSuchFileException e) {
            return false;
        }
    }

    /** A symbolic link's target. By default it fails. */
    public Path readSymbolicLink(Path link) throws IOException {
        throw new UnsupportedOperationException();
    }

    /** It copies `source` to `target`. */
    public abstract void copy(Path source, Path target, CopyOption... options) throws IOException;

    /** It moves `source` to `target`. */
    public abstract void move(Path source, Path target, CopyOption... options) throws IOException;

    /** Whether the two paths name the same file. */
    public abstract boolean isSameFile(Path path, Path path2) throws IOException;

    /** Whether the file is marked hidden. */
    public abstract boolean isHidden(Path path) throws IOException;

    /** The volume the file lives on. */
    public abstract FileStore getFileStore(Path path) throws IOException;

    /**
     * It checks that the file exists and that it can be accessed in the modes asked for.
     *
     * <p>With no modes it checks only that it exists. Returning `void` and throwing is on purpose:
     * the reason for the refusal --it does not exist, or it does and there is no permission-- is
     * information a boolean would lose.
     */
    public abstract void checkAccess(Path path, AccessMode... modes) throws IOException;

    /** A view of the file's attributes, or `null` if the provider does not have it. */
    public abstract <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type,
            LinkOption... options);

    /** It reads the attributes in one go, of the type asked for. */
    public abstract <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type,
            LinkOption... options) throws IOException;

    /** It reads individual attributes by name: `"basic:size,lastModifiedTime"`, `"posix:*"`. */
    public abstract Map<String, Object> readAttributes(Path path, String attributes,
            LinkOption... options) throws IOException;

    /** It sets an attribute by name. */
    public abstract void setAttribute(Path path, String attribute, Object value,
            LinkOption... options) throws IOException;

    /**
     * Whether the file exists.
     *
     * <p>Concrete: it is `checkAccess` with no modes, catching. A provider with a cheaper way of
     * answering it overrides it.
     */
    public boolean exists(Path path, LinkOption... options) {
        try {
            this.checkAccess(path);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * The attributes, or `null` if the file is not there.
     *
     * <p>It exists to save the check-then-read pair, which on top of that has a race in the middle:
     * here the read is a single one and the `null` comes out of the very operation that would have
     * failed.
     */
    public <A extends BasicFileAttributes> A readAttributesIfExists(Path path, Class<A> type,
            LinkOption... options) throws IOException {
        try {
            return this.readAttributes(path, type, options);
        } catch (NoSuchFileException e) {
            return null;
        }
    }
}
