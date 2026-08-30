package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.Writer;

// KajiLibrary's java.io.CharArrayWriter — a Writer that collects into a growable char[].
//
// The sink end of a decorator chain, mirroring ByteArrayOutputStream, and the reason a
// PrintWriter or a BufferedWriter can be exercised without touching a file: wrap one of
// these, write, then read the result back out of memory.
//
// It differs from StringWriter only in what it hands back — a char[] (or a String, or a
// replay into another Writer via writeTo) rather than only a String. That extra exit is
// what makes it useful as a staging area: build the text once, then send it somewhere.
public class CharArrayWriter extends Writer {

    // Written characters are buf[0..count); the array doubles when it fills.
    protected char[] buf;
    protected int count;

    public CharArrayWriter() {
        this.buf = new char[32];
        this.count = 0;
    }

    public CharArrayWriter(int initialSize) {
        if (initialSize < 0) {
            throw new IllegalArgumentException("negative initial size");
        }
        this.buf = new char[initialSize];
        this.count = 0;
    }

    public void write(int c) {
        this.ensureCapacity(this.count + 1);
        this.buf[this.count] = (char) c;
        this.count = this.count + 1;
    }

    public void write(char[] c, int off, int len) {
        this.ensureCapacity(this.count + len);
        System.arraycopy(c, off, this.buf, this.count, len);
        this.count = this.count + len;
    }

    // Overridden so a String is copied straight in; Writer's version would cut a char[]
    // of the slice first, which for a class whose whole job is accumulating is one copy
    // too many.
    public void write(String str, int off, int len) {
        this.ensureCapacity(this.count + len);
        for (int i = 0; i < len; i++) {
            this.buf[this.count + i] = str.charAt(off + i);
        }
        this.count = this.count + len;
    }

    // Replay everything collected into another Writer. This is the composition point: it
    // takes a Writer, so the accumulated text can be poured into a buffered writer, a
    // print writer, or another CharArrayWriter without this class knowing which.
    public void writeTo(Writer out) {
        out.write(this.buf, 0, this.count);
    }

    // Reuse the array instead of allocating a new one — the point of resetting rather than
    // constructing a fresh writer.
    public void reset() {
        this.count = 0;
    }

    // A right-sized copy, so the caller cannot see (or corrupt) our spare capacity.
    public char[] toCharArray() {
        char[] copy = new char[this.count];
        System.arraycopy(this.buf, 0, copy, 0, this.count);
        return copy;
    }

    public int size() {
        return this.count;
    }

    public String toString() {
        return String.valueOf(this.buf, 0, this.count);
    }

    // Both no-ops, and deliberately so: there is nothing downstream to push to and nothing
    // to release. Closing a CharArrayWriter leaves it fully usable, which is the one place
    // in this package where that is true.
    public void flush() {
    }

    public void close() {
    }

    public CharArrayWriter append(CharSequence csq) {
        String s = csq == null ? "null" : csq.toString();
        write(s, 0, s.length());
        return this;
    }

    public CharArrayWriter append(CharSequence csq, int start, int end) {
        CharSequence cs = csq == null ? "null" : csq;
        String s = cs.subSequence(start, end).toString();
        write(s, 0, s.length());
        return this;
    }

    public CharArrayWriter append(char c) {
        write(c);
        return this;
    }

    private void ensureCapacity(int min) {
        if (min > this.buf.length) {
            int newCap = this.buf.length * 2;
            if (newCap < min) {
                newCap = min;
            }
            char[] grown = new char[newCap];
            System.arraycopy(this.buf, 0, grown, 0, this.count);
            this.buf = grown;
        }
    }
}
