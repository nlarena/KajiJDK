package java.nio.file;

import java.io.Closeable;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;

// KajiLibrary's java.nio.file.FileSystem -- a provider of a file system, the factory for Path
// objects. The surface is complete; what the default implementation can and cannot do is said in
// `KajiFileSystem`.
//
// **The three members that were missing are here.** `provider()`, `getFileStores()` and
// `getUserPrincipalLookupService()` were omitted because they named types that did not exist;
// `java.nio.file.spi.FileSystemProvider`, `java.nio.file.FileStore` and
// `java.nio.file.attribute.UserPrincipalLookupService` are written now, so they can be declared.
// The default implementation produces a `FileStore` too, out of `KajiFileStore`; what it still
// cannot produce is a principal lookup service, and that is said in `KajiFileSystem`, where it
// throws `UnsupportedOperationException` -- which is what the spec foresees for a filesystem that
// does not support it.
public abstract class FileSystem implements Closeable {

    /** Initializes a new instance of this class (for subclasses only). */
    protected FileSystem() {
    }

    /** The provider that created this file system. */
    public abstract FileSystemProvider provider();

    // With `throws IOException`, as in the JDK. There was a time when it was not:
    // `java.io.Closeable.close()` did not declare it, and an override cannot widen the checked ones
    // (JLS 8.4.8.3). It declares it now.
    public abstract void close() throws java.io.IOException;

    /** Whether this file system is open. */
    public abstract boolean isOpen();

    /** Whether this file system allows only read access. */
    public abstract boolean isReadOnly();

    /** The name-separator, as a string. */
    public abstract String getSeparator();

    /** The paths of the top-level root directories. */
    public abstract Iterable<Path> getRootDirectories();

    /**
     * The file stores backing this file system.
     *
     * <p>It returns an `Iterable` and not a `List` because enumerating the volumes can be expensive
     * and can fail half way: the spec allows the iteration to throw a wrapped exception, which an
     * already built list could not.
     */
    public abstract Iterable<FileStore> getFileStores();

    /** The names of the file-attribute views this file system supports. */
    public abstract Set<String> supportedFileAttributeViews();

    /** Converts a path string, or a sequence of segments, into a {@link Path}. */
    public abstract Path getPath(String first, String... more);

    /** A {@link PathMatcher} for the given syntax-and-pattern string. */
    public abstract PathMatcher getPathMatcher(String syntaxAndPattern);

    /** The service that looks user and group principals up by name. */
    public abstract UserPrincipalLookupService getUserPrincipalLookupService();

    /** A new {@link WatchService}. */
    public abstract WatchService newWatchService() throws java.io.IOException;
}
