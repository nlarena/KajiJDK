package java.nio.file;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * KajiLibrary's java.nio.file.KajiDirectoryStream -- the {@link DirectoryStream}
 * {@link Files#newDirectoryStream} returns.
 *
 * <p>It reads the directory **once, on construction**, and afterwards iterates over what was read.
 * The JDK does it lazily --it keeps a descriptor open and asks for entries a few at a time-- and
 * the difference shows on a huge directory; the reason for not imitating it is that the native
 * there is, {@code jdk.internal.io.Fs.list}, returns the whole array and not a cursor. Faking
 * laziness over an array already read would be faking.
 *
 * <p>What **is** respected is the observable contract: `iterator()` once
 * ({@link IllegalStateException} the second time), the filter applied on advancing, and an
 * idempotent `close()` that cuts the iteration.
 */
final class KajiDirectoryStream implements DirectoryStream<Path> {

    private final List<Path> entries;
    private final DirectoryStream.Filter<? super Path> filter;
    private boolean iteratorTaken;
    private boolean closed;

    KajiDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        this.filter = filter;
        String[] names = jdk.internal.io.Fs.list(dir.toString());
        if (names == null) {
            // The native returns `null` when it could not list. Why has to be told apart, because
            // the two answers are different exceptions and the caller treats them differently.
            if (!Files.exists(dir)) {
                throw new NoSuchFileException(dir.toString());
            }
            if (!Files.isDirectory(dir)) {
                throw new NotDirectoryException(dir.toString());
            }
            throw new IOException("could not list " + dir);
        }
        this.entries = new ArrayList<Path>();
        int i = 0;
        while (i < names.length) {
            this.entries.add(dir.resolve(names[i]));
            i = i + 1;
        }
    }

    public Iterator<Path> iterator() {
        if (this.closed) {
            throw new IllegalStateException("the stream is closed");
        }
        if (this.iteratorTaken) {
            throw new IllegalStateException("this stream's iterator has already been asked for");
        }
        this.iteratorTaken = true;
        return new Walk();
    }

    public void close() {
        this.closed = true;
    }

    // The filter is applied **on advancing** and not on construction, which is what the contract
    // asks: a filter that fails has to come out as a `DirectoryIteratorException` during the
    // iteration, not before.
    private final class Walk implements Iterator<Path> {

        private int i;
        private Path next;
        private boolean ready;

        public boolean hasNext() {
            if (this.ready) {
                return this.next != null;
            }
            this.ready = true;
            this.next = null;
            while (this.i < KajiDirectoryStream.this.entries.size()) {
                if (KajiDirectoryStream.this.closed) {
                    return false;
                }
                Path p = KajiDirectoryStream.this.entries.get(this.i);
                this.i = this.i + 1;
                boolean passes = true;
                if (KajiDirectoryStream.this.filter != null) {
                    try {
                        passes = KajiDirectoryStream.this.filter.accept(p);
                    } catch (IOException e) {
                        throw new DirectoryIteratorException(e);
                    }
                }
                if (passes) {
                    this.next = p;
                    return true;
                }
            }
            return false;
        }

        public Path next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            Path p = this.next;
            this.ready = false;
            this.next = null;
            return p;
        }
    }
}
