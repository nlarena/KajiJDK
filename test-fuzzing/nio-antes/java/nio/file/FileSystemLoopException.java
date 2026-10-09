package java.nio.file;

// A cycle was detected while walking the directory tree.
//
// KajiJDK never throws it. This note used to blame `Files.walkFileTree` not existing; it exists.
// What it does not do is follow symbolic links --this VM has none-- so no cycle can arise. The type
// is here so the hierarchy is complete.
public class FileSystemLoopException extends FileSystemException {

    private static final long serialVersionUID = 4843039591949217617L;

    /** @param file the path where the cycle closed */
    public FileSystemLoopException(String file) {
        super(file);
    }
}
