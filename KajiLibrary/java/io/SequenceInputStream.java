package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

// KajiLibrary's java.io.SequenceInputStream — several streams read as if they were one.
//
// The other decorators in this package wrap ONE stream and change how its bytes behave;
// this one wraps MANY and changes where they come from, while presenting the same
// InputStream face. That is the same composition property viewed from the other side: a
// reader that only knows `read()` can be fed a file, then a second file, then a trailer
// generated in memory, and never find out.
//
// The streams are taken lazily, from an Enumeration, and this matters: concatenating a
// hundred files must not mean a hundred file handles open at once. We ask for the next
// stream only when the current one runs out, and close it on the way past.
public class SequenceInputStream extends InputStream {

    // The streams still to come.
    private Enumeration<? extends InputStream> e;

    // The one currently being read, or null once the whole sequence is exhausted.
    private InputStream in;

    /**
     * @param e los flujos, en orden
     *
     * <p>El constructor **no** declara `throws IOException` --el JDK tampoco-- y sin embargo
     * `nextStream()` la puede tirar: abrir el primer flujo es E/S. Se envuelve en
     * {@link UncheckedIOException}, que es la unica salida cuando el contrato del constructor dice
     * que no falla.
     */
    public SequenceInputStream(Enumeration<? extends InputStream> e) {
        this.e = e;
        try {
            this.nextStream();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    // The two-stream case is common enough to deserve its own constructor; it is the
    // general one with a two-element sequence behind it.
    public SequenceInputStream(InputStream s1, InputStream s2) {
        this.e = new TwoStreamEnumeration(s1, s2);
        try {
            this.nextStream();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    // Close the stream we are done with and pull the next one, or leave `in` null when
    // there is none. Closing here rather than in close() is what keeps the handle count at
    // one however long the sequence is.
    private void nextStream() throws IOException {
        if (this.in != null) {
            this.in.close();
            this.in = null;
        }
        if (this.e != null && this.e.hasMoreElements()) {
            Object next = this.e.nextElement();
            this.in = (InputStream) next;
        }
    }

    // End of one stream is NOT end of the sequence, so a -1 from the current stream means
    // "advance and try again"; only running out of streams is really the end. The loop is
    // needed rather than a single retry because a stream in the middle may well be empty.
    public int read() throws IOException {
        while (this.in != null) {
            int c = this.in.read();
            if (c >= 0) {
                return c;
            }
            this.nextStream();
        }
        return -1;
    }

    // A bulk read never spans a boundary: it returns what the current stream gave and lets
    // the caller come back. Stitching two streams into one array would be legal but would
    // hide a boundary the caller may care about, and a short read is always allowed.
    public int read(byte[] b, int off, int len) throws IOException {
        if (len <= 0) {
            return 0;
        }
        while (this.in != null) {
            int n = this.in.read(b, off, len);
            if (n > 0) {
                return n;
            }
            this.nextStream();
        }
        return -1;
    }

    // Only the current stream can answer: asking the ones not yet opened would mean
    // opening them, which is exactly what this class is arranged to avoid.
    public int available() throws IOException {
        if (this.in == null) {
            return 0;
        }
        return this.in.available();
    }

    // Closing the sequence closes every stream left in it — the ones already passed were
    // closed by nextStream(), and abandoning the rest unclosed would leak them.
    public void close() throws IOException {
        while (this.in != null) {
            this.nextStream();
        }
        this.e = null;
    }

    public long transferTo(OutputStream out) throws IOException {
        long total = 0;
        byte[] chunk = new byte[8192];
        while (true) {
            int n = read(chunk, 0, chunk.length);
            if (n <= 0) {
                break;
            }
            out.write(chunk, 0, n);
            total = total + n;
        }
        return total;
    }
}

// A two-element Enumeration, so the (InputStream, InputStream) constructor is a special
// case of the general one instead of a second mechanism inside SequenceInputStream. The
// JDK uses a Vector here; two fields are cheaper and drag in no collection. Top-level
// package-private in this same file, which is the shape our javac handles reliably
// (finding #13).
class TwoStreamEnumeration implements Enumeration<InputStream> {

    private InputStream first;
    private InputStream second;
    private int next;

    TwoStreamEnumeration(InputStream first, InputStream second) {
        this.first = first;
        this.second = second;
        this.next = 0;
    }

    public boolean hasMoreElements() {
        return this.next < 2;
    }

    public InputStream nextElement() {
        InputStream s = null;
        if (this.next == 0) {
            s = this.first;
        } else if (this.next == 1) {
            s = this.second;
        }
        this.next = this.next + 1;
        return s;
    }
}
