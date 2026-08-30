package java.nio.file;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

// The single default FileSystem KajiJDK exposes: read-only, root-less, and watch-less, since there
// is no real file system behind it. It exists so Path.getFileSystem() has something to return.
final class KajiFileSystem extends FileSystem {

    static final KajiFileSystem INSTANCE = new KajiFileSystem();

    private KajiFileSystem() {
    }

    public void close() {
    }

    public boolean isOpen() {
        return true;
    }

    public boolean isReadOnly() {
        return true;
    }

    public String getSeparator() {
        return File.separator;
    }

    public Iterable<Path> getRootDirectories() {
        return new ArrayList<Path>();
    }

    public Set<String> supportedFileAttributeViews() {
        return new HashSet<String>();
    }

    public Path getPath(String first, String... more) {
        return Path.of(first, more);
    }

    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException("KajiJDK has no path matchers");
    }

    public WatchService newWatchService() {
        throw new UnsupportedOperationException("KajiJDK has no watch service");
    }
}
