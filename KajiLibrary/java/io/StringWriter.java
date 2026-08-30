package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.Writer;

// KajiLibrary's java.io.StringWriter — a Writer that accumulates everything written into a
// StringBuffer, retrievable via toString() or the live buffer via getBuffer(). Pure Java, and a
// nice dogfood: it drives our own StringBuffer (append/toString).
public class StringWriter extends Writer {

    private final StringBuffer buf;

    public StringWriter() {
        this.buf = new StringBuffer();
    }

    public StringWriter(int initialSize) {
        this.buf = new StringBuffer(initialSize);
    }

    public void write(int c) {
        this.buf.append((char) c);
    }

    public void write(char[] cbuf, int off, int len) {
        int i = 0;
        while (i < len) {
            this.buf.append(cbuf[off + i]);
            i = i + 1;
        }
    }

    public void write(String str) {
        this.buf.append(str);
    }

    public void write(String str, int off, int len) {
        this.buf.append(str.substring(off, off + len));
    }

    public StringWriter append(CharSequence csq) {
        write(csq == null ? "null" : csq.toString());
        return this;
    }

    public StringWriter append(CharSequence csq, int start, int end) {
        CharSequence cs = csq == null ? "null" : csq;
        write(cs.subSequence(start, end).toString());
        return this;
    }

    public StringWriter append(char c) {
        write(c);
        return this;
    }

    /** The live buffer these writes accumulate into. */
    public StringBuffer getBuffer() {
        return this.buf;
    }

    public void flush() {
    }

    public void close() {
    }

    public String toString() {
        return this.buf.toString();
    }
}
