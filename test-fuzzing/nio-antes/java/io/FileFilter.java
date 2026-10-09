package java.io;

// KajiLibrary's java.io.FileFilter -- a predicate over an abstract path name, used by
// File.listFiles(FileFilter). A functional interface.
public interface FileFilter {

    /** Whether {@code pathname} should be accepted. */
    boolean accept(File pathname);
}
