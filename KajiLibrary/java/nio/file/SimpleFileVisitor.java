package java.nio.file;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

// A `FileVisitor` that does nothing and carries on, to inherit from and override only what matters.
//
// **The failures are rethrown, not swallowed.** `visitFileFailed` and `postVisitDirectory` throw
// the exception they receive. It is the prudent choice for a default: a subclass that wants to
// ignore errors has to say so, and not the other way round -- a walk that skips files in silence
// gives an incomplete result without a word.
//
// @param <T> the paths' type
public class SimpleFileVisitor<T> implements FileVisitor<T> {

    /** For the subclasses. */
    protected SimpleFileVisitor() {
    }

    /** It goes into the directory. */
    public FileVisitResult preVisitDirectory(T dir, BasicFileAttributes attrs) throws IOException {
        Objects.requireNonNull(dir);
        Objects.requireNonNull(attrs);
        return FileVisitResult.CONTINUE;
    }

    /** It does nothing with the file. */
    public FileVisitResult visitFile(T file, BasicFileAttributes attrs) throws IOException {
        Objects.requireNonNull(file);
        Objects.requireNonNull(attrs);
        return FileVisitResult.CONTINUE;
    }

    /** It rethrows the failure. */
    public FileVisitResult visitFileFailed(T file, IOException exc) throws IOException {
        Objects.requireNonNull(file);
        throw exc;
    }

    /** It rethrows the failure if there was one; otherwise it carries on. */
    public FileVisitResult postVisitDirectory(T dir, IOException exc) throws IOException {
        Objects.requireNonNull(dir);
        if (exc != null) {
            throw exc;
        }
        return FileVisitResult.CONTINUE;
    }
}
