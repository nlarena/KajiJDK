package java.nio.file;

import java.io.Closeable;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;

// KajiLibrary's java.nio.file.FileSystem -- a provider of a file system, the factory for Path
// objects. La superficie esta completa; que puede y que no puede la implementacion por omision esta
// dicho en `KajiFileSystem`.
//
// **Los tres miembros que faltaban ya estan.** `provider()`, `getFileStores()` y
// `getUserPrincipalLookupService()` estaban omitidos porque nombraban tipos que no existian;
// `java.nio.file.spi.FileSystemProvider`, `java.nio.file.FileStore` y
// `java.nio.file.attribute.UserPrincipalLookupService` ya estan escritos, asi que se pueden
// declarar. Que la implementacion por omision no pueda **producir** un `FileStore` ni un servicio de
// principals es otro asunto, y esta dicho en `KajiFileSystem`: los dos levantan
// `UnsupportedOperationException`, que es lo que la spec preve para un sistema de archivos que no
// los soporta.
public abstract class FileSystem implements Closeable {

    /** Initializes a new instance of this class (for subclasses only). */
    protected FileSystem() {
    }

    /** The provider that created this file system. */
    public abstract FileSystemProvider provider();

    // Con `throws IOException`, como en el JDK. Hubo una epoca en que no: `java.io.Closeable.close()`
    // no lo declaraba, y un override no puede ensanchar las chequeadas (JLS 8.4.8.3). Ya lo declara.
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
     * <p>Devuelve un `Iterable` y no una `List` porque enumerar los volumenes puede ser caro y
     * puede fallar a mitad de camino: la spec permite que la iteracion levante una excepcion
     * envuelta, cosa que una lista ya construida no podria.
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
