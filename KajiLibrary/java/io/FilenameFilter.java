package java.io;

// KajiLibrary's java.io.FilenameFilter -- a predicate over a directory and a candidate file name,
// used by File.list(FilenameFilter) / listFiles(FilenameFilter). A functional interface.
public interface FilenameFilter {

    /** Whether a file named {@code name} in directory {@code dir} should be accepted. */
    boolean accept(File dir, String name);
}
