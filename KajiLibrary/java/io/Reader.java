package java.io;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.io.Closeable;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

// KajiLibrary's java.io.Reader — the abstract superclass of character-input streams. A
// subclass supplies the bulk primitive `read(char[], off, len)` and `close`; the single-char
// and full-array reads are layered on it here.
public abstract class Reader implements Closeable {

    /**
     * El objeto sobre el que este flujo **sincroniza**.
     *
     * <p>Es `protected` y no privado porque una subclase necesita tomarlo: si `Reader` bloqueara
     * sobre `this` y la subclase sobre otra cosa, dos hilos podrian entrar a la vez por caminos
     * distintos. Exponerlo es lo que permite que toda la jerarquia coordine sobre **un** candado.
     *
     * <p>Por defecto es el propio flujo; el constructor de un argumento lo cambia, que es lo que usa
     * un decorador para compartir el candado con el flujo que envuelve.
     */
    protected Object lock;

    /** Sincroniza sobre si mismo. */
    protected Reader() {
        this.lock = this;
    }

    /**
     * Sincroniza sobre `lock`.
     *
     * @throws NullPointerException si `lock` es `null` -- un candado nulo no es
     *     "sin candado", es un fallo que aparece mucho despues
     */
    protected Reader(Object lock) {
        if (lock == null) {
            throw new NullPointerException("lock");
        }
        this.lock = lock;
    }

    // Read up to `len` chars into `cbuf` at `off`; return the count read, or -1 at end.
    public abstract int read(char[] cbuf, int off, int len) throws IOException;

    public abstract void close() throws IOException;

    public int read() throws IOException {
        char[] one = new char[1];
        if (this.read(one, 0, 1) < 0) {
            return -1;
        }
        return one[0];
    }

    public int read(char[] cbuf) throws IOException {
        return this.read(cbuf, 0, cbuf.length);
    }

    // Discard up to `n` characters. Unlike a byte stream we cannot just seek forward, since
    // a Reader sits on top of a decoder whose position is only defined by decoding; so the
    // generic way to skip is to read and throw away, which is what this does.
    public long skip(long n) throws IOException {
        char[] scratch = new char[512];
        long remaining = n;
        boolean atEnd = false;
        while (remaining > 0L && !atEnd) {
            int chunk = 512;
            if (remaining < 512L) {
                chunk = (int) remaining;
            }
            int got = this.read(scratch, 0, chunk);
            if (got < 0) {
                atEnd = true;
            } else {
                remaining = remaining - (long) got;
            }
        }
        return n - remaining;
    }

    // True only if the next read is guaranteed not to block. The conservative answer is
    // false: a source that does not know says "I might block", never the other way round.
    public boolean ready() throws IOException {
        return false;
    }

    // --- mark / reset ---
    //
    // Same idea as InputStream's, in character units: mark a position, read ahead, reset
    // back. The default is "not supported"; a Reader over an in-memory array or one with a
    // buffer to spare overrides all three. (The JDK's mark/reset throw IOException here;
    // KajiLibrary's java.io is still throws-free — see IOException — so this is unchecked.)
    public boolean markSupported() {
        return false;
    }

    public void mark(int readAheadLimit) throws IOException {
        throw new UnsupportedOperationException("mark not supported");
    }

    public void reset() throws IOException {
        throw new UnsupportedOperationException("reset not supported");
    }

    // --- bulk consumption ---

    public int read(CharBuffer target) throws IOException {
        int len = target.remaining();
        char[] cbuf = new char[len];
        int n = this.read(cbuf, 0, len);
        if (n > 0) {
            target.put(cbuf, 0, n);
        }
        return n;
    }

    public long transferTo(Writer out) throws IOException {
        long total = 0;
        char[] chunk = new char[8192];
        while (true) {
            int n = this.read(chunk, 0, chunk.length);
            if (n < 0) {
                break;
            }
            out.write(chunk, 0, n);
            total = total + n;
        }
        return total;
    }

    public String readAllAsString() throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] chunk = new char[8192];
        while (true) {
            int n = this.read(chunk, 0, chunk.length);
            if (n < 0) {
                break;
            }
            sb.append(chunk, 0, n);
        }
        return sb.toString();
    }

    public List<String> readAllLines() throws IOException {
        String all = readAllAsString();
        List<String> lines = new ArrayList<String>();
        int i = 0;
        int start = 0;
        int n = all.length();
        while (i < n) {
            char c = all.charAt(i);
            if (c == '\n' || c == '\r') {
                lines.add(all.substring(start, i));
                if (c == '\r' && i + 1 < n && all.charAt(i + 1) == '\n') {
                    i = i + 1;
                }
                start = i + 1;
            }
            i = i + 1;
        }
        if (start < n) {
            lines.add(all.substring(start, n));
        }
        return lines;
    }

    /** A reader that is always at end of stream. */
    public static Reader nullReader() {
        return new NullReader();
    }

    /** A reader over the characters of {@code cs}. */
    public static Reader of(CharSequence cs) {
        return new StringReader(cs.toString());
    }

    private static final class NullReader extends Reader {
        public int read(char[] cbuf, int off, int len) throws IOException {
            return len == 0 ? 0 : -1;
        }

        public void close() throws IOException {
        }
    }
}
