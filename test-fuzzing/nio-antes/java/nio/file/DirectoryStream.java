package java.nio.file;

import java.io.Closeable;
import java.util.Iterator;

// An open directory, walkable once and to be closed.
//
// **Why it is not a `List` nor a `Stream`.** A directory can have millions of entries; the contract
// is that they are read a few at a time and that the system resource is released with `close()`,
// which is why it extends `Closeable` and why `iterator()` can be called **only once**.
//
// **KajiJDK produces one.** This note used to say it did not, because listing a directory needed a
// native that did not exist; `Fs.list` arrived, `KajiDirectoryStream` implements this interface and
// `Files.newDirectoryStream` returns it.
//
// @param <T> the entries' type
public interface DirectoryStream<T> extends Closeable, Iterable<T> {

    /**
     * The iterator. **One only per stream**: calling it again throws `IllegalStateException`.
     *
     * <p>Its methods declare no `IOException` --`Iterator` does not allow it-- so an I/O failure in
     * the middle of the walk arrives wrapped in a `DirectoryIteratorException`.
     */
    Iterator<T> iterator();

    /**
     * The filter that decides which entries go into the stream.
     *
     * <p>It goes in here and not as a loose interface because it only makes sense alongside
     * `DirectoryStream`; nesting it saves one more generic name in the package.
     *
     * @param <T> the entries' type
     */
    interface Filter<T> {

        /** `true` if the entry is accepted. */
        boolean accept(T entry) throws java.io.IOException;
    }
}
