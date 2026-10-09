package java.nio.file;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;

// The visitor `Files.walkFileTree` calls as it descends the tree.
//
// **The four methods, and why there are four.** A directory is visited twice --before going in and
// after coming out-- because there are tasks that can only be done at each moment: creating the
// target goes in `preVisitDirectory`, deleting the source goes in `postVisitDirectory`. And
// `visitFileFailed` exists so an unreadable file does not cut the whole traversal unless the
// visitor decides so.
//
// **KajiJDK calls it.** This note used to say `Files.walkFileTree` does not exist because walking
// requires listing directories and there was no native; `Fs.list` arrived and both overloads are
// declared.
//
// @param <T> the paths' type, normally `Path`
public interface FileVisitor<T> {

    /**
     * Before going into a directory.
     *
     * @param attrs the directory's attributes
     * @return `CONTINUE` to go in, `SKIP_SUBTREE` to skip it
     */
    FileVisitResult preVisitDirectory(T dir, BasicFileAttributes attrs) throws IOException;

    /** For each file of the directory. */
    FileVisitResult visitFile(T file, BasicFileAttributes attrs) throws IOException;

    /**
     * When a file could not be visited.
     *
     * <p>It receives the exception rather than have it propagate on its own: rethrowing it is one
     * option, carrying on is the other, and the visitor is who decides.
     */
    FileVisitResult visitFileFailed(T file, IOException exc) throws IOException;

    /**
     * On leaving a directory.
     *
     * @param exc `null` if it was walked whole, or the failure that cut it
     */
    FileVisitResult postVisitDirectory(T dir, IOException exc) throws IOException;
}
