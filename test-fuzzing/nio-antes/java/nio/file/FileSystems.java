package java.nio.file;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

// The factory of filesystems.
//
// **KajiJDK has exactly one: the `file` scheme's.** There is no service loading --nothing that
// discovers providers on the class path-- so there is no way of installing a ZIP provider or any
// other. The methods that would create another filesystem **exist and fail** with the exception the
// spec already foresees for the case --`ProviderNotFoundException` when there is nobody to serve
// the scheme, `FileSystemAlreadyExistsException` when creating the one that is already there is
// asked for-- rather than return something.
//
// That `newFileSystem`'s five overloads are here and fail is not filler: code that today opens a
// ZIP with `newFileSystem(path)` compiles, and fails at the one place where the gap can be seen,
// with a message that says so.
public final class FileSystems {

    // Factories only: there is nothing to instantiate.
    private FileSystems() {
    }

    /**
     * The default filesystem, the one that sees the operating system's files.
     *
     * <p>Always the same object, and it cannot be closed: `close()` does nothing and `isOpen()` is
     * always `true`, just as in the JDK. A closeable default filesystem would be an invitation to
     * leave the VM with no disk access from anywhere in the program.
     */
    public static FileSystem getDefault() {
        return KajiFileSystem.INSTANCE;
    }

    private static void fileOnly(URI uri) {
        if (uri == null) {
            throw new NullPointerException();
        }
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase("file")) {
            throw new ProviderNotFoundException("Provider \"" + scheme + "\" not installed");
        }
    }

    /**
     * The filesystem already created for `uri`.
     *
     * @throws ProviderNotFoundException if the scheme is not `file`
     */
    public static FileSystem getFileSystem(URI uri) {
        fileOnly(uri);
        return KajiFileSystem.INSTANCE;
    }

    /**
     * @throws ProviderNotFoundException if the scheme is not `file`
     * @throws FileSystemAlreadyExistsException if it is -- the default filesystem already exists
     */
    public static FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        fileOnly(uri);
        throw new FileSystemAlreadyExistsException();
    }

    /** Like the other; the `ClassLoader` changes nothing because there are no providers to load. */
    public static FileSystem newFileSystem(URI uri, Map<String, ?> env, ClassLoader loader)
            throws IOException {
        return newFileSystem(uri, env);
    }

    /**
     * It would open a file --typically a ZIP-- as a filesystem.
     *
     * @throws ProviderNotFoundException always: KajiJDK has no container provider
     */
    public static FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
        if (path == null) {
            throw new NullPointerException();
        }
        throw new ProviderNotFoundException("no container provider installed for " + path);
    }

    /** Like the other, with no environment. */
    public static FileSystem newFileSystem(Path path) throws IOException {
        return newFileSystem(path, (Map<String, ?>) null);
    }

    /** Like the other; the `ClassLoader` changes nothing. */
    public static FileSystem newFileSystem(Path path, ClassLoader loader) throws IOException {
        return newFileSystem(path, (Map<String, ?>) null);
    }

    /** Like the other; the `ClassLoader` changes nothing. */
    public static FileSystem newFileSystem(Path path, Map<String, ?> env, ClassLoader loader)
            throws IOException {
        return newFileSystem(path, env);
    }
}
