package java.nio.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// KajiLibrary's java.nio.file.Path -- an abstract path in a file system, and the NIO.2 counterpart
// of java.io.File. The path *algebra* (naming, roots, subpaths, resolve/relativize/normalize,
// ordering, iteration) is fully modelled by the default implementation; the file-system-touching
// corners (register with a WatchService, toRealPath) degrade honestly, as KajiJDK has no file
// system. Created via Path.of(...) or File.toPath().
public interface Path extends Comparable<Path>, Iterable<Path>, Watchable {

    /** A path from a path string, or a sequence of segments joined by the default separator. */
    static Path of(String first, String... more) {
        StringBuilder sb = new StringBuilder(first);
        int i = 0;
        while (i < more.length) {
            if (more[i] != null && more[i].length() > 0) {
                sb.append(File.separatorChar).append(more[i]);
            }
            i = i + 1;
        }
        return new KajiPath(sb.toString());
    }

    /** A path from a {@code file:} URI. */
    static Path of(URI uri) {
        if (uri == null) {
            throw new NullPointerException("uri cannot be null");
        }
        String p = uri.getPath();
        if (p == null) {
            throw new IllegalArgumentException("URI has no path: " + uri);
        }
        return new KajiPath(p);
    }

    /** The file system that created this path. */
    FileSystem getFileSystem();

    /** Whether this path is absolute. */
    boolean isAbsolute();

    /** The root component of this path, or null if it has none. */
    Path getRoot();

    /** The last element of this path, or null if it has none. */
    Path getFileName();

    /** The parent of this path, or null. */
    Path getParent();

    /** The number of name elements in this path. */
    int getNameCount();

    /** The name element at {@code index}. */
    Path getName(int index);

    /** A relative path that is a subsequence of the name elements, [{@code begin}, {@code end}). */
    Path subpath(int beginIndex, int endIndex);

    /** Whether this path starts with {@code other}. */
    boolean startsWith(Path other);

    /** Whether this path starts with the path parsed from {@code other}. */
    default boolean startsWith(String other) {
        return this.startsWith(Path.of(other));
    }

    /** Whether this path ends with {@code other}. */
    boolean endsWith(Path other);

    /** Whether this path ends with the path parsed from {@code other}. */
    default boolean endsWith(String other) {
        return this.endsWith(Path.of(other));
    }

    /** This path with redundant {@code .} and {@code ..} elements removed. */
    Path normalize();

    /** Resolves {@code other} against this path. */
    Path resolve(Path other);

    /** Resolves the path parsed from {@code other} against this path. */
    default Path resolve(String other) {
        return this.resolve(Path.of(other));
    }

    /** Resolves a sequence of paths against this path, left to right. */
    default Path resolve(Path first, Path... more) {
        Path result = this.resolve(first);
        int i = 0;
        while (i < more.length) {
            result = result.resolve(more[i]);
            i = i + 1;
        }
        return result;
    }

    /** Resolves a sequence of path strings against this path, left to right. */
    default Path resolve(String first, String... more) {
        Path result = this.resolve(Path.of(first));
        int i = 0;
        while (i < more.length) {
            result = result.resolve(Path.of(more[i]));
            i = i + 1;
        }
        return result;
    }

    /** Resolves {@code other} against this path's parent. */
    default Path resolveSibling(Path other) {
        Path parent = this.getParent();
        return (parent == null) ? other : parent.resolve(other);
    }

    /** Resolves the path parsed from {@code other} against this path's parent. */
    default Path resolveSibling(String other) {
        return this.resolveSibling(Path.of(other));
    }

    /** A relative path from this path to {@code other}. */
    Path relativize(Path other);

    /** A {@code file:} URI for this path. */
    URI toUri();

    /** This path made absolute. */
    Path toAbsolutePath();

    /** The real path of an existing file. KajiJDK has no file system, so this normalizes instead. */
    Path toRealPath(LinkOption... options) throws IOException;

    /** A {@link File} representing this path. */
    default File toFile() {
        return new File(this.toString());
    }

    /** Registers this path with a watch service (unsupported: KajiJDK has no watch service). */
    WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events,
            WatchEvent.Modifier... modifiers) throws IOException;

    /** Registers this path with a watch service for the given event kinds. */
    default WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        return this.register(watcher, events, new WatchEvent.Modifier[0]);
    }

    /** An iterator over the name elements of this path. */
    default Iterator<Path> iterator() {
        List<Path> names = new ArrayList<Path>();
        int n = this.getNameCount();
        int i = 0;
        while (i < n) {
            names.add(this.getName(i));
            i = i + 1;
        }
        return names.iterator();
    }

    int compareTo(Path other);

    boolean equals(Object other);

    int hashCode();

    String toString();
}
