package java.nio.file;

import java.io.Closeable;
import java.util.Set;

// KajiLibrary's java.nio.file.FileSystem -- a provider of a file system, the factory for Path
// objects. KajiJDK models no real file system: the default instance is read-only, exposes no roots,
// and produces no watch service.
//
// A KajiLibrary subset: provider() [java.nio.file.spi.FileSystemProvider], getFileStores()
// [java.nio.file.FileStore] and getUserPrincipalLookupService()
// [java.nio.file.attribute.UserPrincipalLookupService] are OMITTED -- those three abstractions are
// not modelled here (FileSystemProvider alone is a 30-method tree of options and attribute views).
public abstract class FileSystem implements Closeable {

    /** Initializes a new instance of this class (for subclasses only). */
    protected FileSystem() {
    }

    // No `throws IOException`: KajiLibrary's Closeable.close() declares none (an override may not
    // widen the throws clause).
    public abstract void close();

    /** Whether this file system is open. */
    public abstract boolean isOpen();

    /** Whether this file system allows only read access. */
    public abstract boolean isReadOnly();

    /** The name-separator, as a string. */
    public abstract String getSeparator();

    /** The paths of the top-level root directories. */
    public abstract Iterable<Path> getRootDirectories();

    /** The names of the file-attribute views this file system supports. */
    public abstract Set<String> supportedFileAttributeViews();

    /** Converts a path string, or a sequence of segments, into a {@link Path}. */
    public abstract Path getPath(String first, String... more);

    /** A {@link PathMatcher} for the given syntax-and-pattern string. */
    public abstract PathMatcher getPathMatcher(String syntaxAndPattern);

    /** A new {@link WatchService}. */
    public abstract WatchService newWatchService();
}
